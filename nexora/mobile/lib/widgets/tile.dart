import 'package:flutter/material.dart';
import '../theme/tokens.dart';

/// Bento tile, matching components/bento/Tile.tsx. [rule] draws the 3px
/// state bar across the top edge; [feature] applies the hero wash.
class VTile extends StatelessWidget {
  const VTile({
    super.key,
    required this.child,
    this.rule,
    this.feature = false,
    this.onTap,
    this.padding = const EdgeInsets.all(18),
  });

  final Widget child;
  final Color? rule;
  final bool feature;
  final VoidCallback? onTap;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);

    final content = Container(
      decoration: BoxDecoration(
        color: t.panel,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: feature ? t.hairline2 : t.hairline),
        gradient: feature
            ? LinearGradient(
                begin: Alignment.topRight,
                end: Alignment.bottomLeft,
                colors: [t.signalWash, t.panel, t.emberWash],
                stops: const [0, 0.55, 1],
              )
            : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          if (rule != null)
            Container(
              height: 3,
              decoration: BoxDecoration(
                color: rule,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(18)),
              ),
            ),
          Padding(padding: padding, child: child),
        ],
      ),
    );

    if (onTap == null) return content;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: content,
      ),
    );
  }
}

/// Icon + uppercase label row used at the top of most tiles.
class VTileHead extends StatelessWidget {
  const VTileHead({super.key, required this.label, this.icon, this.tone, this.trailing});

  final String label;
  final IconData? icon;
  final Color? tone;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);
    final c = tone ?? t.signal;
    return Row(
      children: [
        if (icon != null) ...[
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: c.withValues(alpha: 0.14),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, size: 17, color: c),
          ),
          const SizedBox(width: 10),
        ],
        Expanded(child: Text(label.toUpperCase(), style: vLabel(t))),
        if (trailing != null) trailing!,
      ],
    );
  }
}

/// Small rounded status pill.
class VPill extends StatelessWidget {
  const VPill({super.key, required this.text, required this.tone});
  final String text;
  final Color tone;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: tone.withValues(alpha: 0.14),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(
          text,
          style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: tone),
        ),
      );
}
