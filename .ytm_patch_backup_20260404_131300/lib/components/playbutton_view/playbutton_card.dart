import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:shadcn_flutter/shadcn_flutter_extension.dart';
import 'package:spotube/collections/spotube_icons.dart';
import 'package:spotube/components/image/universal_image.dart';
import 'package:spotube/extensions/string.dart';
import 'package:spotube/utils/platform.dart';

class PlaybuttonCard extends StatelessWidget {
  final void Function()? onTap;
  final void Function()? onPlaybuttonPressed;
  final void Function()? onAddToQueuePressed;
  final String? description;

  final String? imageUrl;
  final Widget? image;
  final bool isPlaying;
  final bool isLoading;
  final String title;
  final bool isOwner;

  const PlaybuttonCard({
    required this.isPlaying,
    required this.isLoading,
    required this.title,
    this.description,
    this.onPlaybuttonPressed,
    this.onAddToQueuePressed,
    this.onTap,
    this.isOwner = false,
    this.imageUrl,
    this.image,
    super.key,
  }) : assert(
          imageUrl != null || image != null,
          "imageUrl and image can't be null at the same time",
        );

  @override
  Widget build(BuildContext context) {
    final unescapeHtml = description?.unescapeHtml().cleanHtml() ?? '';
    final scale = context.theme.scaling;
    final theme = Theme.of(context);

    return SizedBox(
      width: 158 * scale,
      child: CardImage(
        image: Stack(
          children: [
            if (imageUrl != null)
              Container(
                width: 158 * scale,
                height: 158 * scale,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(18),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withAlpha(70),
                      blurRadius: 18,
                      offset: const Offset(0, 10),
                    ),
                  ],
                  image: DecorationImage(
                    image: UniversalImage.imageProvider(
                      imageUrl!,
                      height: 220 * scale,
                      width: 220 * scale,
                    ),
                    fit: BoxFit.cover,
                  ),
                ),
              )
            else
              SizedBox(
                width: 158 * scale,
                height: 158 * scale,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(18),
                  child: image!,
                ),
              ),
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(18),
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withAlpha(30),
                      Colors.black.withAlpha(125),
                    ],
                  ),
                ),
              ),
            ),
            StatedWidget.builder(
              builder: (context, states) {
                final showControls =
                    states.contains(WidgetState.hovered) ||
                        kIsMobile ||
                        isPlaying ||
                        isLoading;
                return Positioned(
                  right: 10,
                  bottom: 10,
                  child: Column(
                    children: [
                      AnimatedScale(
                        curve: Curves.easeOutBack,
                        duration: const Duration(milliseconds: 300),
                        scale: showControls && !isLoading ? 1 : 0.7,
                        child: AnimatedOpacity(
                          duration: const Duration(milliseconds: 300),
                          opacity: showControls && !isLoading ? 1 : 0,
                          child: IconButton.secondary(
                            icon: const Icon(SpotubeIcons.queueAdd),
                            onPressed: onAddToQueuePressed,
                            size: ButtonSize.small,
                          ),
                        ),
                      ),
                      const Gap(6),
                      AnimatedScale(
                        curve: Curves.easeOutBack,
                        duration: const Duration(milliseconds: 180),
                        scale: showControls ? 1 : 0.78,
                        child: AnimatedOpacity(
                          duration: const Duration(milliseconds: 180),
                          opacity: showControls ? 1 : 0,
                          child: IconButton.primary(
                            icon: switch ((isLoading, isPlaying)) {
                              (true, _) => const CircularProgressIndicator(
                                  size: 15,
                                ),
                              (false, false) => const Icon(SpotubeIcons.play),
                              (false, true) => const Icon(SpotubeIcons.pause)
                            },
                            enabled: !isLoading,
                            onPressed: onPlaybuttonPressed,
                            size: ButtonSize.small,
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
            if (isOwner)
              const Positioned(
                right: 8,
                top: 8,
                child: SecondaryBadge(
                  style: ButtonStyle.secondaryIcon(
                    shape: ButtonShape.circle,
                    size: ButtonSize.small,
                  ),
                  child: Icon(SpotubeIcons.user),
                ),
              ),
          ],
        ),
        title: Tooltip(
          tooltip: TooltipContainer(child: Text(title)).call,
          child: Text(
            title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.typography.small.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        subtitle: Text(
          unescapeHtml.isEmpty ? '
' : unescapeHtml,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: theme.typography.xSmall.copyWith(
            color: theme.colorScheme.mutedForeground,
          ),
        ),
        onPressed: onTap,
      ),
    );
  }
}
