import 'package:auto_route/auto_route.dart';

import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import 'package:spotube/collections/assets.gen.dart';
import 'package:spotube/collections/routes.gr.dart';
import 'package:spotube/components/image/universal_image.dart';
import 'package:spotube/components/links/artist_link.dart';
import 'package:spotube/components/links/link_text.dart';
import 'package:spotube/extensions/constrains.dart';
import 'package:spotube/models/metadata/metadata.dart';
import 'package:spotube/provider/audio_player/audio_player.dart';

class PlayerTrackDetails extends HookConsumerWidget {
  final Color? color;
  final SpotubeTrackObject? track;
  const PlayerTrackDetails({super.key, this.color, this.track});

  @override
  Widget build(BuildContext context, ref) {
    final theme = Theme.of(context);
    final mediaQuery = MediaQuery.of(context);
    final playback = ref.watch(audioPlayerProvider);
    final activeTrack = track ?? playback.activeTrack;

    return Row(
      children: [
        if (activeTrack != null)
          Container(
            padding: const EdgeInsets.all(6),
            constraints: const BoxConstraints(
              maxWidth: 84,
              maxHeight: 84,
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: UniversalImage(
                path: (activeTrack.album.images)
                    .asUrlString(placeholder: ImagePlaceholder.albumArt),
                placeholder: Assets.images.albumPlaceholder.path,
              ),
            ),
          ),
        if (mediaQuery.mdAndDown)
          Flexible(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Text(
                  activeTrack?.name ?? '',
                  overflow: TextOverflow.ellipsis,
                  style: theme.typography.normal.copyWith(
                    color: color,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  activeTrack?.artists.asString() ?? '',
                  overflow: TextOverflow.ellipsis,
                  style: theme.typography.small.copyWith(
                    color: color ?? theme.colorScheme.mutedForeground,
                  ),
                )
              ],
            ),
          ),
        if (mediaQuery.lgAndUp)
          Flexible(
            flex: 1,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LinkText(
                  activeTrack?.name ?? '',
                  TrackRoute(trackId: activeTrack?.id ?? ''),
                  push: true,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontWeight: FontWeight.bold, color: color),
                ),
                ArtistLink(
                  artists: activeTrack?.artists ?? [],
                  onRouteChange: (route) {
                    context.router.navigateNamed(route);
                  },
                  onOverflowArtistClick: () {
                    if (activeTrack == null) return;
                    context.navigateTo(TrackRoute(trackId: activeTrack.id));
                  },
                )
              ],
            ),
          ),
      ],
    );
  }
}
