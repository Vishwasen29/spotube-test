import 'package:flutter/services.dart';
import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:spotube/collections/spotube_icons.dart';
import 'package:spotube/extensions/context.dart';

class ExpandableSearchField extends StatelessWidget {
  final bool isFiltering;
  final ValueChanged<bool> onChangeFiltering;
  final TextEditingController searchController;
  final FocusNode searchFocus;

  const ExpandableSearchField({
    super.key,
    required this.isFiltering,
    required this.onChangeFiltering,
    required this.searchController,
    required this.searchFocus,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedOpacity(
      duration: const Duration(milliseconds: 200),
      opacity: isFiltering ? 1 : 0,
      child: AnimatedSize(
        duration: const Duration(milliseconds: 200),
        child: SizedBox(
          height: isFiltering ? 60 : 0,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: const Color(0xFF1C1C1C),
                borderRadius: BorderRadius.circular(28),
                border: Border.all(color: Colors.white.withAlpha(18)),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: CallbackShortcuts(
                  bindings: {
                    LogicalKeySet(LogicalKeyboardKey.escape): () {
                      onChangeFiltering(false);
                      searchController.clear();
                      searchFocus.unfocus();
                    }
                  },
                  child: TextField(
                    focusNode: searchFocus,
                    controller: searchController,
                    placeholder: Text(context.l10n.search_tracks),
                    features: const [
                      InputFeature.leading(Icon(SpotubeIcons.search))
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class ExpandableSearchButton extends StatelessWidget {
  final bool isFiltering;
  final FocusNode searchFocus;
  final Widget icon;
  final ValueChanged<bool>? onPressed;

  const ExpandableSearchButton({
    super.key,
    required this.isFiltering,
    required this.searchFocus,
    this.icon = const Icon(SpotubeIcons.filter),
    this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: icon,
      variance: isFiltering ? ButtonVariance.secondary : ButtonVariance.ghost,
      onPressed: () {
        if (isFiltering) {
          searchFocus.requestFocus();
        } else {
          searchFocus.unfocus();
        }
        onPressed?.call(!isFiltering);
      },
    );
  }
}
