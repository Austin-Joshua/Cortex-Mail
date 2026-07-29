import 'package:flutter/material.dart';

import '../models/models.dart';
import '../state/app_state.dart';
import '../theme/tokens.dart';
import '../widgets/tile.dart';

/// Inbox with a category filter rail. On a tablet the list is constrained to
/// a readable measure rather than stretching edge to edge.
class InboxScreen extends StatefulWidget {
  const InboxScreen({super.key, required this.state, this.priorityOnly = false});

  final AppState state;
  final bool priorityOnly;

  @override
  State<InboxScreen> createState() => _InboxScreenState();
}

class _InboxScreenState extends State<InboxScreen> {
  String? _category;
  String _search = '';

  List<VEmail> get _visible {
    var list = widget.priorityOnly ? widget.state.priorityStream : widget.state.emails;
    if (_category != null) list = list.where((e) => e.category == _category).toList();
    if (_search.isNotEmpty) {
      final q = _search.toLowerCase();
      list = list
          .where((e) =>
              e.title.toLowerCase().contains(q) || e.sender.toLowerCase().contains(q))
          .toList();
    }
    return list;
  }

  @override
  Widget build(BuildContext context) {
    final t = VTokens.of(context);
    final counts = widget.state.categoryCounts;
    final active = counts.entries.where((e) => e.value > 0).toList()
      ..sort((a, b) => b.value.compareTo(a.value));

    return RefreshIndicator(
      color: t.signal,
      backgroundColor: t.panel,
      onRefresh: widget.state.refresh,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 760),
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 8, 14, 6),
                child: TextField(
                  onChanged: (v) => setState(() => _search = v),
                  style: TextStyle(fontSize: 14, color: t.ink),
                  decoration: InputDecoration(
                    hintText: 'Search mail…',
                    hintStyle: TextStyle(color: t.ink3, fontSize: 14),
                    prefixIcon: Icon(Icons.search, size: 19, color: t.ink3),
                    filled: true,
                    fillColor: t.panel,
                    contentPadding: const EdgeInsets.symmetric(vertical: 12),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(color: t.hairline),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(color: t.signal),
                    ),
                  ),
                ),
              ),
              if (active.isNotEmpty)
                SizedBox(
                  height: 44,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    children: [
                      _chip(t, 'All', null, widget.state.emails.length),
                      for (final e in active)
                        _chip(t, categoryOf(e.key).label, e.key, e.value),
                    ],
                  ),
                ),
              Expanded(
                child: _visible.isEmpty
                    ? ListView(
                        children: [
                          const SizedBox(height: 80),
                          Center(
                            child: Text(
                              widget.state.loading ? 'Loading…' : 'Nothing here.',
                              style: TextStyle(color: t.ink3, fontSize: 13),
                            ),
                          ),
                        ],
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.fromLTRB(14, 6, 14, 24),
                        itemCount: _visible.length,
                        itemBuilder: (context, i) => Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: _card(t, _visible[i]),
                        ),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _chip(VTokens t, String label, String? key, int count) {
    final on = _category == key;
    final tone = key == null ? t.signal : categoryOf(key).color;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: GestureDetector(
        onTap: () => setState(() => _category = key),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: on ? tone : t.panel,
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: on ? tone : t.hairline),
          ),
          child: Row(
            children: [
              if (!on) ...[
                Container(
                  width: 7,
                  height: 7,
                  decoration: BoxDecoration(color: tone, shape: BoxShape.circle),
                ),
                const SizedBox(width: 7),
              ],
              Text('$label  $count',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: on ? t.onSignal : t.ink2,
                  )),
            ],
          ),
        ),
      ),
    );
  }

  Widget _card(VTokens t, VEmail e) {
    final cat = categoryOf(e.category);
    final tone = switch (e.priority) {
      'HIGH' => t.ember,
      'MEDIUM' => t.signal,
      _ => t.ink4,
    };

    return VTile(
      onTap: () => widget.state.api.markRead(e.id).catchError((_) {}),
      padding: const EdgeInsets.all(14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            margin: const EdgeInsets.only(top: 5),
            width: 7,
            height: 7,
            decoration: BoxDecoration(
              color: e.isRead ? t.hairline2 : tone,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  e.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: e.isRead ? FontWeight.w500 : FontWeight.w700,
                    color: t.ink,
                  ),
                ),
                const SizedBox(height: 4),
                Text(e.sender,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 12, color: t.ink3)),
                const SizedBox(height: 8),
                Row(
                  children: [
                    VPill(text: cat.label, tone: cat.color),
                    const Spacer(),
                    Text(_ago(e.receivedAt), style: TextStyle(fontSize: 11.5, color: t.ink3)),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _ago(DateTime? at) {
    if (at == null) return '';
    final mins = DateTime.now().difference(at).inMinutes;
    if (mins < 60) return '${mins < 1 ? 1 : mins}m';
    if (mins < 1440) return '${mins ~/ 60}h';
    return '${mins ~/ 1440}d';
  }
}
