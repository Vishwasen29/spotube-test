import 'dart:math';

import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart' show Badge;
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:shadcn_flutter/shadcn_flutter_extension.dart';

import 'package:spotube/collections/side_bar_tiles.dart';
import 'package:spotube/extensions/constrains.dart';
import 'package:spotube/extensions/context.dart';
import 'package:spotube/models/database/database.dart';
import 'package:spotube/provider/download_manager_provider.dart';
import 'package:spotube/provider/user_preferences/user_preferences_provider.dart';

final navigationPanelHeight = StateProvider<double>((ref) => 50);

class SpotubeNavigationBar extends HookConsumerWidget {
  const SpotubeNavigationBar({
    super.key,
  });

  @override
  Widget build(BuildContext context, ref) {
    final mediaQuery = MediaQuery.of(context);
    final theme = Theme.of(context);

    final downloadCount = ref
        .watch(downloadManagerProvider)
        .where((e) =>
            e.status == DownloadStatus.downloading ||
            e.status == DownloadStatus.queued)
        .length;
    final layoutMode =
        ref.watch(userPreferencesProvider.select((s) => s.layoutMode));

    final navbarTileList = useMemoized(
      () => getNavbarTileList(context.l10n),
      [context.l10n],
    );

    final panelHeight = ref.watch(navigationPanelHeight);

    final router = context.watchRouter;
    final selectedIndex = max(
      0,
      navbarTileList.indexWhere(
        (e) => router.currentPath.startsWith(e.pathPrefix),
      ),
    );

    if (layoutMode == LayoutMode.extended ||
        (mediaQuery.mdAndUp && layoutMode == LayoutMode.adaptive) ||
        panelHeight < 10) {
      return const SizedBox();
    }

    return AnimatedContainer(
      duration: const Duration(milliseconds: 150),
      curve: Curves.easeOut,
      height: min(panelHeight + 30, 86),
      padding: const EdgeInsets.fromLTRB(10, 0, 10, 8),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: const Color(0xE61A1A1A),
          borderRadius: BorderRadius.circular(28),
          border: Border.all(color: Colors.white.withAlpha(20)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(90),
              blurRadius: 24,
              offset: const Offset(0, 12),
            ),
          ],
        ),
        child: NavigationBar(
          index: selectedIndex,
          surfaceBlur: context.theme.surfaceBlur,
          surfaceOpacity: 1,
          children: [
            for (final tile in navbarTileList)
              NavigationButton(
                style: navbarTileList[selectedIndex] == tile
                    ? const ButtonStyle.fixed(
                        density: ButtonDensity.compact,
                        size: ButtonSize.small,
                      )
                    : const ButtonStyle.muted(
                        density: ButtonDensity.compact,
                        size: ButtonSize.small,
                      ),
                child: Builder(
                  builder: (context) {
                    final selected = navbarTileList[selectedIndex] == tile;
                    final color = selected
                        ? const Color(0xFFFF4E45)
                        : theme.colorScheme.mutedForeground;
                    return Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Badge(
                          isLabelVisible:
                              tile.id == "library" && downloadCount > 0,
                          label: Text(downloadCount.toString()),
                          child: Icon(tile.icon, color: color),
                        ),
                        const Gap(2),
                        Text(
                          tile.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.typography.xSmall.copyWith(
                            color: color,
                            fontWeight:
                                selected ? FontWeight.w700 : FontWeight.w500,
                          ),
                        ),
                      ],
                    );
                  },
                ),
                onPressed: () {
                  context.navigateTo(tile.route);
                },
              )
          ],
        ),
      ),
    );
  }
}
