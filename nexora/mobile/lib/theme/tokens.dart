import 'package:flutter/material.dart';

/// Velocity's "Instrument Cluster" palette, ported 1:1 from the web app's
/// styles/bento.css so the two clients cannot drift apart. Antique gold is
/// the primary, ember red carries urgency, on warm oxblood grounds.
///
/// Gold inverts between themes — deep enough to carry white text on the
/// light paper, pale enough to carry dark text on the dark ground — so
/// [onSignal] flips with it.
class VTokens {
  const VTokens({
    required this.ground,
    required this.ground2,
    required this.panel,
    required this.panel2,
    required this.hairline,
    required this.hairline2,
    required this.ink,
    required this.ink2,
    required this.ink3,
    required this.ink4,
    required this.signal,
    required this.signalDim,
    required this.signalWash,
    required this.onSignal,
    required this.ember,
    required this.emberWash,
    required this.pulse,
    required this.pulseWash,
  });

  final Color ground, ground2, panel, panel2, hairline, hairline2;
  final Color ink, ink2, ink3, ink4;
  final Color signal, signalDim, signalWash, onSignal;
  final Color ember, emberWash;
  final Color pulse, pulseWash;

  static const light = VTokens(
    ground: Color(0xFFFAF7F3),
    ground2: Color(0xFFF1EBE2),
    panel: Color(0xFFFFFFFF),
    panel2: Color(0xFFFBF8F4),
    hairline: Color(0xFFE8DFD2),
    hairline2: Color(0xFFD3C6B4),
    ink: Color(0xFF1C1310),
    ink2: Color(0xFF5A4A42),
    ink3: Color(0xFF8B7A70),
    ink4: Color(0xFFB5A69C),
    signal: Color(0xFFA8761A),
    signalDim: Color(0xFF8A5F10),
    signalWash: Color(0x1CA8761A),
    onSignal: Color(0xFFFFFFFF),
    ember: Color(0xFFC1272D),
    emberWash: Color(0x1AC1272D),
    pulse: Color(0xFF1F7A5C),
    pulseWash: Color(0x1F1F7A5C),
  );

  static const dark = VTokens(
    ground: Color(0xFF0E0709),
    ground2: Color(0xFF170C10),
    panel: Color(0xFF1A0F13),
    panel2: Color(0xFF241519),
    hairline: Color(0xFF38222A),
    hairline2: Color(0xFF52323C),
    ink: Color(0xFFF8F0EE),
    ink2: Color(0xFFCBB6B4),
    ink3: Color(0xFF9C8683),
    ink4: Color(0xFF705E5C),
    signal: Color(0xFFE8C06A),
    signalDim: Color(0xFFF2D089),
    signalWash: Color(0x21E8C06A),
    onSignal: Color(0xFF241505),
    ember: Color(0xFFF0555F),
    emberWash: Color(0x26F0555F),
    pulse: Color(0xFF35C99A),
    pulseWash: Color(0x2435C99A),
  );

  static VTokens of(BuildContext context) =>
      Theme.of(context).brightness == Brightness.dark ? dark : light;
}

/// Category hues, matching utils/catColors.ts. Warm-forward, holding no
/// indigo or violet, with a jade/teal/dusty-blue kept in for separation
/// since these are data colours and must stay distinguishable.
const Map<String, ({String label, Color color})> kCategories = {
  'ASSIGNMENT': (label: 'Assignment', color: Color(0xFFC8912B)),
  'HACKATHON': (label: 'Hackathon', color: Color(0xFFE0703A)),
  'PLACEMENT': (label: 'Placement', color: Color(0xFF3E9E74)),
  'MEETING': (label: 'Meeting', color: Color(0xFFB5506B)),
  'ATTENDANCE': (label: 'Attendance', color: Color(0xFFD0453F)),
  'ANNOUNCEMENT': (label: 'Announcement', color: Color(0xFFD9A441)),
  'PROMOTIONAL': (label: 'Promo', color: Color(0xFF94837A)),
  'INTERNSHIP': (label: 'Internship', color: Color(0xFF3E9B9B)),
  'RESEARCH': (label: 'Research', color: Color(0xFF6C86AE)),
  'FINANCE': (label: 'Finance', color: Color(0xFF6E9E45)),
  'PERSONAL': (label: 'Personal', color: Color(0xFFD4788E)),
  'SPAM': (label: 'Spam', color: Color(0xFF9E3B36)),
  'UNCATEGORIZED': (label: 'Other', color: Color(0xFF94837A)),
};

({String label, Color color}) categoryOf(String? key) =>
    kCategories[key ?? ''] ?? kCategories['UNCATEGORIZED']!;

/// Uppercase micro-label used throughout the instrument surfaces.
TextStyle vLabel(VTokens t) => TextStyle(
      fontSize: 10,
      fontWeight: FontWeight.w700,
      letterSpacing: 1.4,
      color: t.ink3,
      height: 1.35,
    );

/// Big tabular readout. Digits must not jitter as values change.
TextStyle vReadout(VTokens t, {double size = 34, Color? color}) => TextStyle(
      fontSize: size,
      fontWeight: FontWeight.w800,
      letterSpacing: -size * 0.03,
      color: color ?? t.ink,
      height: 1,
      fontFeatures: const [FontFeature.tabularFigures()],
    );

ThemeData buildTheme(Brightness brightness) {
  final t = brightness == Brightness.dark ? VTokens.dark : VTokens.light;
  return ThemeData(
    brightness: brightness,
    scaffoldBackgroundColor: t.ground,
    colorScheme: ColorScheme.fromSeed(
      seedColor: t.signal,
      brightness: brightness,
      primary: t.signal,
      onPrimary: t.onSignal,
      surface: t.panel,
      onSurface: t.ink,
      error: t.ember,
    ),
    dividerColor: t.hairline,
    useMaterial3: true,
    fontFamily: 'Roboto',
  );
}
