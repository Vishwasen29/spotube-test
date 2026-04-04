import 'package:auto_route/auto_route.dart';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:sliding_up_panel/sliding_up_panel.dart';

import 'package:spotube/collections/assets.gen.dart';
import 'package:spotube/collections/routes.gr.dart';
import 'package:spotube/collections/spotube_icons.dart';
import 'package:spotube/components/framework/app_pop_scope.dart';
import 'package:spotube/models/metadata/metadata.dart';
import 'package:spotube/modules/player/player_actions.dart';
import 'package:spotube/modules/player/player_controls.dart';
import 'package:spotube/modules/player/volume_slider.dart';
import 'package:spotube/components/dialogs/track_details_dialog.dart';
import 'package:spotube/components/links/artist_link.dart';
import 'package:spotube/components/titlebar/titlebar.dart';
import 'package:spotube/components/image/universal_image.dart';
import 'package:spotube/extensions/constrains.dart';
import 'package:spotube/extensions/context.dart';
import 'package:spotube/modules/root/spotube_navigation_bar.dart';
import 'package:spotube/provider/audio_player/audio_player.dart';
import 'package:spotube/provider/metadata_plugin/audio_source/quality_label.dart';
import 'package:spotube/provider/server/active_track_sources.dart';
import 'package:spotube/provider/volume_provider.dart';

class PlayerView extends HookConsumerWidget {
  final PanelController panelController;
  final ScrollController scrollController;
  const PlayerView({
    super.key,
    required this.panelController,
    required this.scrollController,
  });

  @override
  Widget build(BuildContext context, ref) {
    final theme = Theme.of(context);
    final sourcedCurrentTrack = ref.watch(activeTrackSourcesProvider);
    final currentActiveTrack =
        ref.watch(audioPlayerProvider.select((s) => s.activeTrack));
    final currentActiveTrackSource = sourcedCurrentTrack.asData?.value?.source;
    final isLocalTrack = currentActiveTrack is SpotubeLocalTrackObject;
    final mediaQuery = MediaQuery.sizeOf(context);
    final qualityLabel = ref.watch(audioSourceQualityLabelProvider);

    final shouldHide = useState(true);

    ref.listen(navigationPanelHeight, (_, height) {
      shouldHide.value = height.ceil() == 50;
    });

    if (shouldHide.value) {
      return const SizedBox();
    }

    useEffect(() {
      if (mediaQuery.lgAndUp) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          panelController.close();
        });
      }
      return null;
    }, [mediaQuery.lgAndUp]);

    String albumArt = useMemoized(
      () => (currentActiveTrack?.album.images).asUrlString(
        placeholder: ImagePlaceholder.albumArt,
      ),
      [currentActiveTrack?.album.images],
    );

    useEffect(() {
      for (final renderView in WidgetsBinding.instance.renderViews) {
        renderView.automaticSystemUiAdjustment = false;
      }

      return () {
        for (final renderView in WidgetsBinding.instance.renderViews) {
          renderView.automaticSystemUiAdjustment = true;
        }
      };
    }, [panelController.isAttached && panelController.isPanelOpen]);

    return AppPopScope(
      canPop: false,
      onPopInvoked: (didPop) async {
        await panelController.close();
      },
      child: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Color(0xFF181818),
              Color(0xFF101010),
              Color(0xFF090909),
            ],
          ),
        ),
        child: SurfaceCard(
          borderWidth: 0,
          borderRadius: BorderRadius.zero,
          surfaceOpacity: 0.16,
          padding: EdgeInsets.zero,
          child: Scaffold(
            backgroundColor: Colors.transparent,
            headers: [
              SafeArea(
                bottom: false,
                child: TitleBar(
                  surfaceOpacity: 0,
                  surfaceBlur: 0,
                  leading: [
                    IconButton.ghost(
                      size: const ButtonSize(1.2),
                      icon: const Icon(SpotubeIcons.angleDown),
                      onPressed: panelController.close,
                    )
                  ],
                  trailing: [
                    if (!isLocalTrack)
                      Tooltip(
                        tooltip: TooltipContainer(
                          child: Text(context.l10n.details),
                        ).call,
                        child: IconButton.ghost(
                          size: const ButtonSize(1.2),
                          icon: const Icon(SpotubeIcons.info),
                          onPressed: currentActiveTrackSource == null
                              ? null
                              : () {
                                  showDialog(
                                      context: context,
                                      builder: (context) {
                                        return TrackDetailsDialog(
                                          track: currentActiveTrack
                                              as SpotubeFullTrackObject,
                                        );
                                      });
                                },
                        ),
                      )
                  ],
                ),
              ),
            ],
            child: SingleChildScrollView(
              controller: scrollController,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
                child: Column(
                  children: [
                    Container(
                      margin: const EdgeInsets.all(8),
                      constraints:
                          const BoxConstraints(maxHeight: 320, maxWidth: 320),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(28),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withAlpha(120),
                            spreadRadius: 2,
                            blurRadius: 28,
                            offset: const Offset(0, 18),
                          ),
                        ],
                      ),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(28),
                        child: UniversalImage(
                          path: albumArt,
                          placeholder: Assets.images.albumPlaceholder.path,
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                    const SizedBox(height: 28),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                      alignment: Alignment.centerLeft,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          AutoSizeText(
                            currentActiveTrack?.name ?? context.l10n.not_playing,
                            style: theme.typography.h3.copyWith(
                              fontWeight: FontWeight.w800,
                            ),
                            maxFontSize: 24,
                            maxLines: 2,
                            textAlign: TextAlign.start,
                          ),
                          const SizedBox(height: 6),
                          if (isLocalTrack)
                            Text(
                              currentActiveTrack.artists.asString(),
                              style: theme.typography.normal.copyWith(
                                fontWeight: FontWeight.w600,
                                color: theme.colorScheme.mutedForeground,
                              ),
                            )
                          else
                            ArtistLink(
                              artists: currentActiveTrack?.artists ?? [],
                              textStyle: theme.typography.normal.copyWith(
                                fontWeight: FontWeight.w600,
                                color: theme.colorScheme.mutedForeground,
                              ),
                              onRouteChange: (route) {
                                panelController.close();
                                context.router.navigateNamed(route);
                              },
                              onOverflowArtistClick: () => context.navigateTo(
                                TrackRoute(
                                  trackId: currentActiveTrack!.id,
                                ),
                              ),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    const PlayerControls(),
                    const SizedBox(height: 20),
                    const PlayerActions(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      showQueue: false,
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        const SizedBox(width: 10),
                        Expanded(
                          child: OutlineButton(
                            leading: const Icon(SpotubeIcons.queue),
                            child: Text(context.l10n.queue),
                            onPressed: () {
                              context.pushRoute(const PlayerQueueRoute());
                            },
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: OutlineButton(
                            leading: const Icon(SpotubeIcons.music),
                            child: Text(context.l10n.lyrics),
                            onPressed: () {
                              context.pushRoute(const PlayerLyricsRoute());
                            },
                          ),
                        ),
                        const SizedBox(width: 10),
                      ],
                    ),
                    const SizedBox(height: 18),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                      child: Consumer(builder: (context, ref, _) {
                        final volume = ref.watch(volumeProvider);
                        return VolumeSlider(
                          fullWidth: true,
                          value: volume,
                          onChanged: (value) {
                            ref.read(volumeProvider.notifier).setVolume(value);
                          },
                        );
                      }),
                    ),
                    const Gap(18),
                    OutlineBadge(
                      style: const ButtonStyle.outline(
                        size: ButtonSize.normal,
                        density: ButtonDensity.dense,
                        shape: ButtonShape.rectangle,
                      ).copyWith(
                        textStyle: (context, states, value) {
                          return value.copyWith(fontWeight: FontWeight.w600);
                        },
                      ),
                      leading: const Icon(SpotubeIcons.lightningOutlined),
                      child: Text(qualityLabel),
                    )
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
