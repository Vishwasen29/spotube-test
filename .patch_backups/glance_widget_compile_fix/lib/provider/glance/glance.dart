import 'dart:convert';

import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:home_widget/home_widget.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:http/http.dart';
import 'package:logger/logger.dart';
import 'package:spotube/models/metadata/metadata.dart';
import 'package:spotube/provider/audio_player/audio_player.dart';
import 'package:spotube/provider/server/server.dart';
import 'package:spotube/services/audio_player/audio_player.dart';
import 'package:spotube/services/logger/logger.dart';
import 'package:spotube/utils/platform.dart';

@pragma("vm:entry-point")
Future<void> glanceBackgroundCallback(Uri? data) async {
  final logger = Logger();
  try {
    if (data == null ||
        data.host != "playback" ||
        data.pathSegments.isEmpty ||
        data.queryParameters["serverAddress"] == null) {
      return;
    }

    final command = data.pathSegments.first;
    final res = await get(
      Uri.parse(
        "http://${data.queryParameters["serverAddress"]}/playback/$command",
      ),
    );

    if (res.statusCode != 200) {
      throw Exception("Failed to execute command: $command\nBody: ${res.body}");
    }
  } catch (e) {
    logger.e("[GlanceBackgroundCallback] $e");
  }
}

Future<bool?> _saveWidgetData<T>(String key, T? value) async {
  try {
    if (!kIsMobile) return null;
    return await HomeWidget.saveWidgetData<T>(key, value);
  } catch (e, stack) {
    AppLogger.reportError(e, stack);
    return null;
  }
}

Future<void> _updateWidget() async {
  try {
    if (!kIsMobile) return;

    if (kIsAndroid) {
      await HomeWidget.updateWidget(
        androidName: 'HomePlayerWidgetReceiver',
        qualifiedAndroidName:
            'oss.krtirtho.spotube.glance.HomePlayerWidgetReceiver',
      );
    }
    if (kIsIOS) {
      await HomeWidget.updateWidget(
        name: 'HomePlayerWidget',
        iOSName: 'HomePlayerWidget',
      );
    }
  } on Exception catch (e, stack) {
    AppLogger.reportError(e, stack);
  }
}

Future<void> _sendActiveTrack(SpotubeTrackObject? track) async {
  if (track == null) {
    await _saveWidgetData("activeTrack", null);
    await _updateWidget();
    return;
  }

  final jsonTrack = track.toJson();

  final image = track.album.images.firstOrNull;
  final cachedImage = image == null
      ? null
      : image.url.startsWith("http")
          ? (await DefaultCacheManager().getSingleFile(image.url)).path
          : image.url;

  final data = {
    ...jsonTrack,
    "album": {
      ...jsonTrack["album"],
      "images": [
        if (cachedImage != null && image != null)
          {
            ...image.toJson(),
            "path": cachedImage,
          }
      ]
    }
  };

  await _saveWidgetData("activeTrack", jsonEncode(data));
  await _updateWidget();
}

Future<void> _sendQueueState(AudioPlayerStateLike state) async {
  await _saveWidgetData("currentIndex", state.currentIndex < 0 ? 0 : state.currentIndex + 1);
  await _saveWidgetData("queueLength", state.tracks.length);
  await _saveWidgetData("isShuffled", state.shuffled);
  await _saveWidgetData("loopMode", state.loopMode.name);
  await _updateWidget();
}

typedef AudioPlayerStateLike = ({
  int currentIndex,
  List<SpotubeTrackObject> tracks,
  bool shuffled,
  dynamic loopMode,
});

final glanceProvider = Provider((ref) {
  final server = ref.read(serverProvider);
  final playerState = ref.read(audioPlayerProvider);
  final activeTrack = playerState.activeTrack;

  server.whenData(
    (value) async {
      final (:server, :port) = value;
      await _saveWidgetData(
        "playbackServerAddress",
        "${server.address.host}:$port",
      );
      await _updateWidget();
    },
  );

  _sendActiveTrack(activeTrack);
  _sendQueueState((
    currentIndex: playerState.currentIndex,
    tracks: playerState.tracks,
    shuffled: playerState.shuffled,
    loopMode: playerState.loopMode,
  ));

  ref.listen(serverProvider, (prev, next) async {
    next.whenData(
      (value) async {
        final (:server, :port) = value;
        await _saveWidgetData(
          "playbackServerAddress",
          "${server.address.host}:$port",
        );
        await _updateWidget();
      },
    );
  });

  ref.listen(
    audioPlayerProvider,
    (previous, next) async {
      try {
        if (previous?.activeTrack != next.activeTrack) {
          await _sendActiveTrack(next.activeTrack);
        }

        if (previous?.currentIndex != next.currentIndex ||
            previous?.tracks.length != next.tracks.length ||
            previous?.shuffled != next.shuffled ||
            previous?.loopMode != next.loopMode) {
          await _sendQueueState((
            currentIndex: next.currentIndex,
            tracks: next.tracks,
            shuffled: next.shuffled,
            loopMode: next.loopMode,
          ));
        }
      } catch (e, stack) {
        AppLogger.reportError(e, stack);
      }
    },
  );

  final subscriptions = [
    audioPlayer.playingStream.listen((playing) async {
      await _saveWidgetData("isPlaying", playing);
      await _saveWidgetData(
        "positionTimestampMs",
        DateTime.now().millisecondsSinceEpoch,
      );
      await _updateWidget();
    }),
    audioPlayer.positionStream.listen((position) async {
      await _saveWidgetData("position", position.inSeconds);
      await _saveWidgetData(
        "positionTimestampMs",
        DateTime.now().millisecondsSinceEpoch,
      );
      await _updateWidget();
    }),
    audioPlayer.durationStream.listen((duration) async {
      await _saveWidgetData("duration", duration.inSeconds);
      await _updateWidget();
    }),
    audioPlayer.shuffledStream.listen((shuffled) async {
      await _saveWidgetData("isShuffled", shuffled);
      await _updateWidget();
    }),
    audioPlayer.loopModeStream.listen((loopMode) async {
      await _saveWidgetData("loopMode", loopMode.name);
      await _updateWidget();
    }),
    audioPlayer.currentIndexChangedStream.listen((index) async {
      final current = ref.read(audioPlayerProvider);
      await _saveWidgetData("currentIndex", index < 0 ? 0 : index + 1);
      await _saveWidgetData("queueLength", current.tracks.length);
      await _updateWidget();
    }),
    audioPlayer.playlistStream.listen((playlist) async {
      await _saveWidgetData("queueLength", playlist.medias.length);
      await _updateWidget();
    }),
  ];

  ref.onDispose(() {
    for (final subscription in subscriptions) {
      subscription.cancel();
    }
  });
});
