import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../theme/tokens.dart';

/// The 270° instrument arc, matching components/bento/Gauge.tsx on the web.
/// Sweeps in once on mount rather than snapping to its value.
class VGauge extends StatefulWidget {
  const VGauge({super.key, required this.value, this.size = 168, this.label = 'of 100'});

  final int value;
  final double size;
  final String label;

  @override
  State<VGauge> createState() => _VGaugeState();
}

class _VGaugeState extends State<VGauge> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1100),
  );
  late Animation<double> _a = _curve(0, widget.value.toDouble());

  Animation<double> _curve(double from, double to) => Tween(begin: from, end: to)
      .animate(CurvedAnimation(parent: _c, curve: Curves.easeOutCubic));

  @override
  void initState() {
    super.initState();
    _c.forward();
  }

  @override
  void didUpdateWidget(covariant VGauge old) {
    super.didUpdateWidget(old);
    if (old.value != widget.value) {
      _a = _curve(_a.value, widget.value.toDouble());
      _c.forward(from: 0);
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);
    final tone = widget.value >= 75
        ? t.pulse
        : widget.value >= 45
            ? t.signal
            : t.ember;

    return SizedBox(
      width: widget.size,
      height: widget.size,
      child: AnimatedBuilder(
        animation: _a,
        builder: (context, _) => CustomPaint(
          painter: _ArcPainter(value: _a.value, track: t.hairline, tone: tone),
          child: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  _a.value.round().toString(),
                  style: vReadout(t, size: widget.size * 0.3),
                ),
                const SizedBox(height: 6),
                Text(widget.label.toUpperCase(), style: vLabel(t)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ArcPainter extends CustomPainter {
  _ArcPainter({required this.value, required this.track, required this.tone});

  final double value;
  final Color track, tone;

  // 270° sweep opening at the bottom, same geometry as the web gauge.
  static const _start = math.pi * 0.75;
  static const _sweep = math.pi * 1.5;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = size.width * 0.062;
    final rect = Rect.fromLTWH(stroke / 2, stroke / 2, size.width - stroke, size.height - stroke);

    final base = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke
      ..strokeCap = StrokeCap.round
      ..color = track;
    canvas.drawArc(rect, _start, _sweep, false, base);

    if (value > 0) {
      canvas.drawArc(
        rect,
        _start,
        _sweep * (value.clamp(0, 100) / 100),
        false,
        base..color = tone,
      );
    }
  }

  @override
  bool shouldRepaint(_ArcPainter old) =>
      old.value != value || old.tone != tone || old.track != track;
}
