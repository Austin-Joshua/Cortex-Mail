import 'package:flutter/material.dart';

import 'screens/dashboard_screen.dart';
import 'screens/inbox_screen.dart';
import 'state/app_state.dart';
import 'theme/tokens.dart';

void main() => runApp(const VelocityApp());

class VelocityApp extends StatefulWidget {
  const VelocityApp({super.key});

  @override
  State<VelocityApp> createState() => _VelocityAppState();
}

class _VelocityAppState extends State<VelocityApp> {
  final AppState _state = AppState();

  @override
  void initState() {
    super.initState();
    _state.bootstrap();
  }

  @override
  void dispose() {
    _state.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Velocity',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(Brightness.light),
      darkTheme: buildTheme(Brightness.dark),
      // Follows the OS setting, matching the web client.
      themeMode: ThemeMode.system,
      home: AnimatedBuilder(
        animation: _state,
        builder: (context, _) =>
            _state.isAuthenticated ? HomeShell(state: _state) : SignInScreen(state: _state),
      ),
    );
  }
}

/// Navigation adapts to width: a bottom bar on phones, a side rail on
/// tablets. One shell serves both form factors.
class HomeShell extends StatefulWidget {
  const HomeShell({super.key, required this.state});
  final AppState state;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  static const _destinations = [
    (icon: Icons.dashboard_outlined, selected: Icons.dashboard, label: 'Home'),
    (icon: Icons.inbox_outlined, selected: Icons.inbox, label: 'Inbox'),
    (icon: Icons.bolt_outlined, selected: Icons.bolt, label: 'Priority'),
  ];

  Widget _body() => switch (_index) {
        1 => InboxScreen(state: widget.state, key: const ValueKey('inbox')),
        2 => InboxScreen(state: widget.state, priorityOnly: true, key: const ValueKey('priority')),
        _ => DashboardScreen(state: widget.state),
      };

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);
    final useRail = MediaQuery.sizeOf(context).width >= 720;
    final body = SafeArea(child: _body());

    return Scaffold(
      backgroundColor: t.ground,
      appBar: AppBar(
        backgroundColor: t.ground,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        titleSpacing: 14,
        title: const Wordmark(),
        actions: [
          if (widget.state.loading)
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Center(
                child: SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2, color: t.signal),
                ),
              ),
            )
          else
            IconButton(
              tooltip: 'Sync inbox',
              onPressed: widget.state.syncInbox,
              icon: Icon(Icons.refresh, color: t.ink2, size: 21),
            ),
          IconButton(
            tooltip: 'Sign out',
            onPressed: widget.state.signOut,
            icon: Icon(Icons.logout, color: t.ink2, size: 19),
          ),
          const SizedBox(width: 4),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1),
          child: Container(height: 1, color: t.hairline),
        ),
      ),
      body: useRail
          ? Row(
              children: [
                NavigationRail(
                  backgroundColor: t.ground,
                  selectedIndex: _index,
                  onDestinationSelected: (i) => setState(() => _index = i),
                  labelType: NavigationRailLabelType.all,
                  indicatorColor: t.signalWash,
                  selectedIconTheme: IconThemeData(color: t.signal),
                  unselectedIconTheme: IconThemeData(color: t.ink3),
                  selectedLabelTextStyle:
                      TextStyle(color: t.signal, fontSize: 12, fontWeight: FontWeight.w700),
                  unselectedLabelTextStyle: TextStyle(color: t.ink3, fontSize: 12),
                  destinations: [
                    for (final d in _destinations)
                      NavigationRailDestination(
                        icon: Icon(d.icon),
                        selectedIcon: Icon(d.selected),
                        label: Text(d.label),
                      ),
                  ],
                ),
                Container(width: 1, color: t.hairline),
                Expanded(child: body),
              ],
            )
          : body,
      bottomNavigationBar: useRail
          ? null
          : Container(
              decoration: BoxDecoration(border: Border(top: BorderSide(color: t.hairline))),
              child: NavigationBar(
                backgroundColor: t.ground,
                surfaceTintColor: Colors.transparent,
                indicatorColor: t.signalWash,
                selectedIndex: _index,
                onDestinationSelected: (i) => setState(() => _index = i),
                destinations: [
                  for (final d in _destinations)
                    NavigationDestination(
                      icon: Icon(d.icon, color: t.ink3),
                      selectedIcon: Icon(d.selected, color: t.signal),
                      label: d.label,
                    ),
                ],
              ),
            ),
    );
  }
}

class SignInScreen extends StatelessWidget {
  const SignInScreen({super.key, required this.state});
  final AppState state;

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);

    return Scaffold(
      backgroundColor: t.ground,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Wordmark(size: 34),
                  const SizedBox(height: 34),
                  Text(
                    'Your inbox has a speed.',
                    style: TextStyle(
                      fontSize: 33,
                      height: 1.05,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -1.4,
                      color: t.ink,
                    ),
                  ),
                  Text(
                    'You have never seen it.',
                    style: TextStyle(
                      fontSize: 33,
                      height: 1.05,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -1.4,
                      color: t.signal,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Velocity puts an instrument on your Gmail — the drag your '
                    'backlog is creating, and the deadlines hiding inside it.',
                    style: TextStyle(fontSize: 14.5, height: 1.55, color: t.ink2),
                  ),
                  const SizedBox(height: 28),
                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: FilledButton.icon(
                      style: FilledButton.styleFrom(
                        backgroundColor: t.signal,
                        foregroundColor: t.onSignal,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                      ),
                      onPressed: () => _showTokenSheet(context),
                      icon: const Icon(Icons.mail_outline, size: 19),
                      label: const Text('Connect Gmail',
                          style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5)),
                    ),
                  ),
                  const SizedBox(height: 14),
                  Text(
                    'Read-only access · Velocity cannot send, delete or alter mail',
                    style: TextStyle(fontSize: 12, color: t.ink3),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// Google OAuth runs in the backend's browser flow, which hands back a JWT.
  /// Until the deep-link callback is registered on both platforms the token is
  /// pasted here — deliberately explicit rather than pretending a native OAuth
  /// flow exists.
  void _showTokenSheet(BuildContext context) {
    final t = VTokens.of(context);
    final controller = TextEditingController();

    showModalBottomSheet<void>(
      context: context,
      backgroundColor: t.panel,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(22))),
      builder: (sheetContext) => Padding(
        padding:
            EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.viewInsetsOf(sheetContext).bottom + 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('PASTE YOUR SESSION TOKEN', style: vLabel(t)),
            const SizedBox(height: 12),
            Text(
              'Sign in on the Velocity website, then copy the token from '
              'Settings. Native Google sign-in lands with deep linking.',
              style: TextStyle(fontSize: 13, color: t.ink2, height: 1.5),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              autofocus: true,
              style: TextStyle(color: t.ink, fontSize: 14),
              decoration: InputDecoration(
                hintText: 'eyJhbGciOi…',
                hintStyle: TextStyle(color: t.ink4),
                filled: true,
                fillColor: t.panel2,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide(color: t.hairline),
                ),
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 46,
              child: FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: t.signal,
                  foregroundColor: t.onSignal,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                onPressed: () {
                  final token = controller.text.trim();
                  if (token.isEmpty) return;
                  Navigator.of(sheetContext).pop();
                  state.signIn(token);
                },
                child: const Text('Continue', style: TextStyle(fontWeight: FontWeight.w700)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Gauge-ring monogram plus wide-tracked wordmark, matching the web lockup.
class Wordmark extends StatelessWidget {
  const Wordmark({super.key, this.size = 26});
  final double size;

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: size,
          height: size,
          child: CustomPaint(painter: _MarkPainter(gold: t.signal, ember: t.ember)),
        ),
        const SizedBox(width: 10),
        Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'VELOCITY',
              style: TextStyle(
                fontSize: size * 0.46,
                fontWeight: FontWeight.w600,
                letterSpacing: size * 0.19,
                color: t.ink,
              ),
            ),
            const SizedBox(height: 3),
            Container(
              height: 1,
              width: size * 3.05,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [t.signal, t.ember, Colors.transparent],
                  stops: const [0, 0.65, 1],
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _MarkPainter extends CustomPainter {
  _MarkPainter({required this.gold, required this.ember});
  final Color gold, ember;

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width;
    final ring = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = s * 0.05
      ..strokeCap = StrokeCap.round
      ..color = gold.withValues(alpha: 0.5);
    // 270° arc opening at the bottom, same geometry as the SVG mark.
    canvas.drawArc(
      Rect.fromLTWH(s * 0.09, s * 0.09, s * 0.82, s * 0.82),
      2.356,
      4.712,
      false,
      ring,
    );

    final v = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = s * 0.095
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..color = gold;
    canvas.drawPath(
      Path()
        ..moveTo(s * 0.325, s * 0.35)
        ..lineTo(s * 0.5, s * 0.645)
        ..lineTo(s * 0.675, s * 0.35),
      v,
    );

    canvas.drawCircle(Offset(s * 0.5, s * 0.82), s * 0.062, Paint()..color = ember);
  }

  @override
  bool shouldRepaint(_MarkPainter old) => old.gold != gold || old.ember != ember;
}
