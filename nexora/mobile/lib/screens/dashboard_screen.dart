import 'package:flutter/material.dart';

import '../models/models.dart';
import '../state/app_state.dart';
import '../theme/tokens.dart';
import '../widgets/gauge.dart';
import '../widgets/tile.dart';

/// Bento dashboard. Column count follows the width the same way the web
/// grid does: 1 up on a narrow phone, 2 up on a normal phone, 4 up on a
/// tablet — so one layout serves every size instead of a phone build and a
/// separate tablet build.
class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key, required this.state});

  final AppState state;

  static int columnsFor(double width) {
    if (width >= 900) return 4;
    if (width >= 380) return 2;
    return 1;
  }

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);
    final s = state.summary;

    return RefreshIndicator(
      color: t.signal,
      backgroundColor: t.panel,
      onRefresh: state.refresh,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final cols = columnsFor(constraints.maxWidth);
          final wide = cols >= 4;

          return CustomScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            slivers: [
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(14, 8, 14, 4),
                sliver: SliverToBoxAdapter(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${_greeting()}, ${state.user?.firstName ?? 'there'}',
                        style: TextStyle(
                          fontSize: wide ? 30 : 25,
                          fontWeight: FontWeight.w800,
                          letterSpacing: -0.9,
                          color: t.ink,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        s == null
                            ? 'Loading your inbox…'
                            : '${s.verdict} · ${state.emails.length} messages synced',
                        style: TextStyle(fontSize: 13.5, color: t.ink3),
                      ),
                    ],
                  ),
                ),
              ),

              if (state.error != null)
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(14, 10, 14, 0),
                  sliver: SliverToBoxAdapter(
                    child: VTile(
                      rule: t.ember,
                      child: Row(
                        children: [
                          Icon(Icons.warning_amber_rounded, size: 18, color: t.ember),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(state.error!,
                                style: TextStyle(fontSize: 13, color: t.ember)),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),

              // Hero — always full width, it is the instrument.
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(14, 14, 14, 0),
                sliver: SliverToBoxAdapter(child: _hero(context, s)),
              ),

              SliverPadding(
                padding: const EdgeInsets.fromLTRB(14, 12, 14, 0),
                sliver: SliverGrid.count(
                  crossAxisCount: cols,
                  mainAxisSpacing: 12,
                  crossAxisSpacing: 12,
                  childAspectRatio: cols == 1 ? 2.6 : 1.18,
                  children: [
                    _metric(context, 'Unread', '${s?.unreadCount ?? 0}',
                        '${state.emails.length} synced', Icons.inbox_outlined, t.signal),
                    _metric(context, 'Deadlines', '${s?.upcomingDeadlines.length ?? 0}',
                        _overdueNote(s), Icons.timer_outlined, t.ember),
                    _metric(context, 'Actions', '${s?.pendingActions.length ?? 0}',
                        'from your mail', Icons.checklist_rtl, t.pulse),
                    _volume(context),
                  ],
                ),
              ),

              SliverPadding(
                padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
                sliver: SliverToBoxAdapter(child: _needsYou(context)),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _hero(BuildContext context, DashboardSummary? s) {
    final t = VTokens.of(context);
    final score = s?.velocityScore ?? 0;
    final tone = score >= 75 ? t.pulse : (score >= 45 ? t.signal : t.ember);

    return VTile(
      feature: true,
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 20),
      child: Column(
        children: [
          VTileHead(
            label: 'Velocity Score',
            icon: Icons.speed_rounded,
            tone: tone,
            trailing: VPill(text: s?.verdict ?? '—', tone: tone),
          ),
          const SizedBox(height: 16),
          VGauge(value: score),
          const SizedBox(height: 18),
          Row(
            children: [
              _chip(context, 'Backlog', '${s?.unreadCount ?? 0}', t.signal),
              const SizedBox(width: 8),
              _chip(context, 'Actions', '${s?.pendingActions.length ?? 0}', t.signal),
              const SizedBox(width: 8),
              _chip(context, 'Overdue', '${s?.overdueCount ?? 0}',
                  (s?.overdueCount ?? 0) > 0 ? t.ember : t.ink4),
            ],
          ),
        ],
      ),
    );
  }

  Widget _chip(BuildContext context, String label, String value, Color tone) {
    final t = VTokens.of(context);
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: t.panel2,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: t.hairline),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(value, style: vReadout(t, size: 19, color: tone)),
            const SizedBox(height: 6),
            Text(label.toUpperCase(), style: vLabel(t), overflow: TextOverflow.ellipsis),
          ],
        ),
      ),
    );
  }

  Widget _metric(BuildContext context, String label, String value, String note,
      IconData icon, Color tone) {
    final t = VTokens.of(context);
    return VTile(
      rule: tone,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          VTileHead(label: label, icon: icon, tone: tone),
          const SizedBox(height: 10),
          Text(value, style: vReadout(t, size: 28)),
          const SizedBox(height: 6),
          Text(note,
              style: TextStyle(fontSize: 11.5, color: t.ink3),
              maxLines: 1,
              overflow: TextOverflow.ellipsis),
        ],
      ),
    );
  }

  Widget _volume(BuildContext context) {
    final t = VTokens.of(context);
    final week = state.weekVolume;
    final peak = week.fold<int>(1, (m, v) => v > m ? v : m);
    const labels = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
    final todayIdx = DateTime.now().weekday % 7;

    return VTile(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          VTileHead(label: 'Volume · 7d', icon: Icons.show_chart, tone: t.signal),
          const SizedBox(height: 10),
          SizedBox(
            height: 34,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                for (var i = 0; i < week.length; i++) ...[
                  Expanded(
                    child: Container(
                      height: (week[i] / peak * 30).clamp(3, 30).toDouble(),
                      decoration: BoxDecoration(
                        color: week[i] == peak && week[i] > 0 ? t.signal : t.signalWash,
                        borderRadius: BorderRadius.circular(3),
                      ),
                    ),
                  ),
                  if (i < week.length - 1) const SizedBox(width: 4),
                ],
              ],
            ),
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              for (var i = 0; i < 7; i++)
                Expanded(
                  child: Text(
                    labels[(todayIdx - 6 + i + 14) % 7],
                    textAlign: TextAlign.center,
                    style: vLabel(t),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _needsYou(BuildContext context) {
    final t = VTokens.of(context);
    final items = state.priorityStream.take(6).toList();

    return VTile(
      padding: EdgeInsets.zero,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 18, 18, 12),
            child: VTileHead(
                label: 'Needs you first', icon: Icons.local_fire_department_outlined, tone: t.ember),
          ),
          if (items.isEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 0, 18, 24),
              child: Text(
                state.loading ? 'Reading your inbox…' : 'Inbox clear. Nothing waiting on you.',
                style: TextStyle(fontSize: 13, color: t.ink3),
              ),
            )
          else
            for (final e in items) _row(context, e),
        ],
      ),
    );
  }

  Widget _row(BuildContext context, VEmail e) {
    final t = VTokens.of(context);
    final cat = categoryOf(e.category);
    final tone = switch (e.priority) {
      'HIGH' => t.ember,
      'MEDIUM' => t.signal,
      _ => t.ink4,
    };

    return Container(
      decoration: BoxDecoration(border: Border(top: BorderSide(color: t.hairline))),
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
      child: Row(
        children: [
          Container(
            width: 7,
            height: 7,
            decoration: BoxDecoration(color: tone, shape: BoxShape.circle),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(e.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: t.ink)),
                const SizedBox(height: 2),
                Text('${e.sender} · ${cat.label}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 11.5, color: t.ink3)),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Text(_ago(e.receivedAt),
              style: TextStyle(
                  fontSize: 11.5, color: t.ink3, fontFeatures: const [FontFeature.tabularFigures()])),
        ],
      ),
    );
  }

  String _overdueNote(DashboardSummary? s) {
    if (s == null) return '—';
    final n = s.overdueCount;
    return n > 0 ? '$n overdue' : 'none overdue';
  }

  static String _greeting() {
    final h = DateTime.now().hour;
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  }

  static String _ago(DateTime? at) {
    if (at == null) return '';
    final mins = DateTime.now().difference(at).inMinutes;
    if (mins < 60) return '${mins < 1 ? 1 : mins}m';
    if (mins < 1440) return '${mins ~/ 60}h';
    return '${mins ~/ 1440}d';
  }
}
