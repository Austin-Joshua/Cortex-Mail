import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:velocity_mobile/main.dart';
import 'package:velocity_mobile/models/models.dart';
import 'package:velocity_mobile/screens/dashboard_screen.dart';
import 'package:velocity_mobile/state/app_state.dart';
import 'package:velocity_mobile/theme/tokens.dart';
import 'package:velocity_mobile/widgets/gauge.dart';

DashboardSummary _summary({
  int unread = 0,
  int actions = 0,
  List<Map<String, dynamic>> deadlines = const [],
}) =>
    DashboardSummary.fromJson({
      'unreadCount': unread,
      'pendingActions': List.generate(actions, (i) => {'id': i}),
      'upcomingDeadlines': deadlines,
      'categoryCounts': <String, dynamic>{},
    });

void main() {
  group('Velocity Score', () {
    test('a clear inbox scores 100', () {
      expect(_summary().velocityScore, 100);
      expect(_summary().verdict, 'Running clear');
    });

    test('debits backlog, actions and overdue deadlines', () {
      // 10 unread = -12, 2 actions = -6  ->  82
      expect(_summary(unread: 10, actions: 2).velocityScore, 82);
    });

    test('each debit is capped so one factor cannot sink the score', () {
      // 500 unread would be -600 uncapped; the backlog debit caps at 45.
      expect(_summary(unread: 500).velocityScore, 55);
    });

    test('never falls below zero', () {
      final past = DateTime.now().subtract(const Duration(days: 3)).toIso8601String();
      final s = _summary(
        unread: 999,
        actions: 999,
        deadlines: List.generate(99, (_) => {'dueDate': past}),
      );
      expect(s.velocityScore, greaterThanOrEqualTo(0));
      expect(s.verdict, 'Backlog building');
    });

    test('counts only deadlines already in the past as overdue', () {
      final past = DateTime.now().subtract(const Duration(days: 1)).toIso8601String();
      final future = DateTime.now().add(const Duration(days: 5)).toIso8601String();
      final s = _summary(deadlines: [
        {'dueDate': past},
        {'dueDate': future},
        {'dueDate': future},
      ]);
      expect(s.overdueCount, 1);
    });

    test('matches the web client formula on a shared case', () {
      // Same inputs the web dashboard renders as 55.
      final past = DateTime.now().subtract(const Duration(days: 1)).toIso8601String();
      final s = _summary(unread: 23, actions: 4, deadlines: [
        {'dueDate': past},
      ]);
      expect(s.velocityScore, 55);
    });
  });

  group('VEmail', () {
    test('falls back to the address when no sender name is present', () {
      final e = VEmail.fromJson({'id': 1, 'senderEmail': 'a@b.com'});
      expect(e.sender, 'a@b.com');
      expect(e.title, '(no subject)');
    });

    test('ranks HIGH ahead of MEDIUM ahead of LOW', () {
      int rank(String p) => VEmail.fromJson({'id': 1, 'priority': p}).priorityRank;
      expect(rank('HIGH'), lessThan(rank('MEDIUM')));
      expect(rank('MEDIUM'), lessThan(rank('LOW')));
    });

    test('survives a payload with missing keys', () {
      final e = VEmail.fromJson({'id': 7});
      expect(e.id, 7);
      expect(e.isRead, isFalse);
      expect(e.receivedAt, isNull);
    });
  });

  group('AppState', () {
    test('orders the priority stream by band, then newest first', () {
      final state = AppState();
      final now = DateTime.now();
      state.emails = [
        VEmail(id: 1, priority: 'LOW', isRead: false, receivedAt: now),
        VEmail(id: 2, priority: 'HIGH', isRead: false, receivedAt: now.subtract(const Duration(hours: 5))),
        VEmail(id: 3, priority: 'HIGH', isRead: false, receivedAt: now),
        VEmail(id: 4, priority: 'HIGH', isRead: true, receivedAt: now),
      ];
      final ids = state.priorityStream.map((e) => e.id).toList();
      expect(ids, [3, 2, 1], reason: 'read mail is excluded, HIGH leads, newest first');
    });

    test('buckets the week volume by day', () {
      final state = AppState();
      final today = DateTime.now();
      state.emails = [
        VEmail(id: 1, receivedAt: today),
        VEmail(id: 2, receivedAt: today),
        VEmail(id: 3, receivedAt: today.subtract(const Duration(days: 2))),
        VEmail(id: 4, receivedAt: today.subtract(const Duration(days: 40))),
      ];
      final week = state.weekVolume;
      expect(week.length, 7);
      expect(week.last, 2, reason: 'today is the final bucket');
      expect(week.reduce((a, b) => a + b), 3, reason: 'the 40-day-old mail falls outside');
    });
  });

  group('Layout', () {
    test('column count steps with width', () {
      expect(DashboardScreen.columnsFor(360), 1);
      expect(DashboardScreen.columnsFor(390), 2);
      expect(DashboardScreen.columnsFor(1024), 4);
    });
  });

  group('Theme', () {
    test('gold inverts between themes so text contrast holds', () {
      expect(VTokens.light.signal.computeLuminance(),
          lessThan(VTokens.dark.signal.computeLuminance()));
      // Light mode puts white on gold; dark mode puts dark ink on gold.
      expect(VTokens.light.onSignal.computeLuminance(),
          greaterThan(VTokens.dark.onSignal.computeLuminance()));
    });

    test('every category resolves, unknown keys included', () {
      expect(categoryOf('HACKATHON').label, 'Hackathon');
      expect(categoryOf('NOT_A_CATEGORY').label, 'Other');
      expect(categoryOf(null).label, 'Other');
    });
  });

  group('Widgets', () {
    testWidgets('gauge renders its value', (tester) async {
      await tester.pumpWidget(
        MaterialApp(theme: buildTheme(Brightness.dark), home: const Scaffold(body: VGauge(value: 72))),
      );
      await tester.pumpAndSettle();
      expect(find.text('72'), findsOneWidget);
      expect(find.text('OF 100'), findsOneWidget);
    });

    testWidgets('unauthenticated launch shows the sign-in screen', (tester) async {
      await tester.pumpWidget(const VelocityApp());
      await tester.pump();
      expect(find.text('Connect Gmail'), findsOneWidget);
      expect(find.byType(HomeShell), findsNothing);
    });

    testWidgets('phone width uses the bottom bar, tablet width uses the rail',
        (tester) async {
      final state = AppState();

      tester.view.physicalSize = const Size(390 * 3, 844 * 3);
      tester.view.devicePixelRatio = 3.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(
        MaterialApp(theme: buildTheme(Brightness.dark), home: HomeShell(state: state)),
      );
      await tester.pump();
      expect(find.byType(NavigationBar), findsOneWidget);
      expect(find.byType(NavigationRail), findsNothing);

      tester.view.physicalSize = const Size(1024 * 2, 768 * 2);
      tester.view.devicePixelRatio = 2.0;
      await tester.pumpWidget(
        MaterialApp(theme: buildTheme(Brightness.dark), home: HomeShell(state: state)),
      );
      await tester.pump();
      expect(find.byType(NavigationRail), findsOneWidget);
      expect(find.byType(NavigationBar), findsNothing);
    });
  });
}
