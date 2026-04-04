import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:sliding_up_panel/sliding_up_panel.dart';
import 'package:spotube/collections/intents.dart';
import 'package:spotube/collections/spotube_icons.dart';
import 'package:spotube/modules/player/player_track_details.dart';
import 'package:spotube/modules/root/spotube_navigation_bar.dart';
import 'package:spotube/provider/audio_player/audio_player.dart';
import 'package:spotube/provider/audio_player/querying_track_info.dart';
import 'package:spotube/services/audio_player/audio_player.dart';

class PlayerOverlayCollapsedSection extends HookConsumerWidget {
  final PanelController panelController;
  const PlayerOverlayCollapsedSection({
    super.key,
    required this.panelController,
  });

  @override
  Widget build(BuildContext context, ref) {
    final playlist = ref.watch(audioPlayerProvider);
    final canShow = playlist.activeTrack != null;

    final isFetchingActiveTrack = ref.watch(queryingTrackInfoProvider);
    final playing =
        useStream(audioPlayer.playingStream).data ?? audioPlayer.isPlaying;

    final theme = Theme.of(context);

    final shouldShow = useState(true);

    ref.listen(navigationPanelHeight, (_, height) {
      shouldShow.value = height.ceil() == 50;
    });

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 250),
      child: canShow && shouldShow.value
          ? Padding(
              padding: const EdgeInsets.fromLTRB(8, 6, 8, 5),
              child: SurfaceCard(
                surfaceBlur: theme.surfaceBlur,
                surfaceOpacity: 0.98,
                padding: EdgeInsets.zero,
                borderRadius: BorderRadius.circular(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 42,
                      height: 4,
                      margin: const EdgeInsets.only(top: 8, bottom: 6),
                      decoration: BoxDecoration(
                        color: Colors.white.withAlpha(60),
                        borderRadius: BorderRadius.circular(99),
                      ),
                    ),
                    Expanded(
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                panelController.open();
                              },
                              child: Container(
                                width: double.infinity,
                                color: Colors.transparent,
                                child: PlayerTrackDetails(
                                  track: playlist.activeTrack,
                                  color: theme.colorScheme.foreground,
                                ),
                              ),
                            ),
                          ),
                          Row(
                            children: [
                              IconButton.ghost(
                                icon: const Icon(SpotubeIcons.skipBack),
                                onPressed: isFetchingActiveTrack
                                    ? null
                                    : audioPlayer.skipToPrevious,
                              ),
                              Consumer(
                                builder: (context, ref, _) {
                                  return IconButton.primary(
                                    icon: isFetchingActiveTrack
                                        ? const SizedBox(
                                            height: 20,
                                            width: 20,
                                            child: CircularProgressIndicator(),
                                          )
                                        : Icon(
                                            playing
                                                ? SpotubeIcons.pause
                                                : SpotubeIcons.play,
                                          ),
                                    onPressed: Actions.handler<PlayPauseIntent>(
                                      context,
                                      PlayPauseIntent(ref),
                                    ),
                                  );
                                },
                              ),
                              IconButton.ghost(
                                icon: const Icon(SpotubeIcons.skipForward),
                                onPressed: isFetchingActiveTrack
                                    ? null
                                    : audioPlayer.skipToNext,
                              ),
                              const Gap(5),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            )
          : const SizedBox.shrink(),
    );
  }
}
