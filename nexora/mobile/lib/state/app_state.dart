import 'package:flutter/foundation.dart';

import '../api/client.dart';
import '../models/models.dart';

/// Single source of truth for the session. Deliberately a plain
/// ChangeNotifier — the app has one screenful of shared state and does not
/// need a state-management dependency to hold it.
class AppState extends ChangeNotifier {
  AppState({VelocityApi? api}) : api = api ?? VelocityApi();

  final VelocityApi api;

  bool loading = false;
  String? error;

  VUser? user;
  DashboardSummary? summary;
  List<VEmail> emails = const [];
  Map<String, int> categoryCounts = const {};

  bool get isAuthenticated => api.isAuthenticated;

  int get unreadCount => emails.where((e) => !e.isRead).length;

  /// Highest-signal unread mail, newest first inside each priority band.
  List<VEmail> get priorityStream {
    final unread = emails.where((e) => !e.isRead).toList()
      ..sort((a, b) {
        final byPriority = a.priorityRank.compareTo(b.priorityRank);
        if (byPriority != 0) return byPriority;
        final at = a.receivedAt ?? DateTime.fromMillisecondsSinceEpoch(0);
        final bt = b.receivedAt ?? DateTime.fromMillisecondsSinceEpoch(0);
        return bt.compareTo(at);
      });
    return unread;
  }

  /// Messages received per day for the last 7 days, oldest first.
  List<int> get weekVolume {
    final today = DateTime.now();
    final start = DateTime(today.year, today.month, today.day).subtract(const Duration(days: 6));
    final buckets = List.filled(7, 0);
    for (final e in emails) {
      final at = e.receivedAt;
      if (at == null) continue;
      final idx = DateTime(at.year, at.month, at.day).difference(start).inDays;
      if (idx >= 0 && idx < 7) buckets[idx]++;
    }
    return buckets;
  }

  Future<void> bootstrap() async {
    await api.loadToken();
    if (api.isAuthenticated) await refresh();
    notifyListeners();
  }

  Future<void> signIn(String token) async {
    await api.setToken(token);
    await refresh();
  }

  Future<void> signOut() async {
    await api.logout();
    user = null;
    summary = null;
    emails = const [];
    categoryCounts = const {};
    notifyListeners();
  }

  Future<void> refresh() async {
    loading = true;
    error = null;
    notifyListeners();
    try {
      final results = await Future.wait([
        api.me(),
        api.dashboardSummary(),
        api.emails(size: 50),
        api.categoryCounts(),
      ]);
      user = results[0] as VUser;
      summary = results[1] as DashboardSummary;
      emails = results[2] as List<VEmail>;
      categoryCounts = results[3] as Map<String, int>;
    } on ApiException catch (e) {
      error = e.statusCode == 401 ? 'Session expired. Sign in again.' : e.message;
      if (e.statusCode == 401) await api.setToken(null);
    } catch (e) {
      error = 'Could not reach Velocity. Check your connection.';
    } finally {
      loading = false;
      notifyListeners();
    }
  }

  Future<void> syncInbox() async {
    loading = true;
    notifyListeners();
    try {
      await api.sync();
      await refresh();
    } on ApiException catch (e) {
      error = e.message;
      loading = false;
      notifyListeners();
    }
  }
}
