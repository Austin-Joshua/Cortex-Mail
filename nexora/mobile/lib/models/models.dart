library;

/// Wire models mirroring the backend DTOs. Every field is defensive about
/// nulls — the API omits keys rather than sending null in several places.

DateTime? _date(dynamic v) {
  if (v == null) return null;
  if (v is String) return DateTime.tryParse(v);
  return null;
}

int _int(dynamic v, [int fallback = 0]) {
  if (v is num) return v.toInt();
  if (v is String) return int.tryParse(v) ?? fallback;
  return fallback;
}

class VUser {
  VUser({required this.email, this.name, this.userRole, this.lastSyncedAt});

  final String email;
  final String? name;
  final String? userRole;
  final DateTime? lastSyncedAt;

  String get firstName => (name ?? '').split(' ').first.isEmpty ? 'there' : name!.split(' ').first;

  String get initials {
    final parts = (name ?? '').trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
    if (parts.isEmpty) return 'VL';
    return parts.take(2).map((p) => p[0].toUpperCase()).join();
  }

  factory VUser.fromJson(Map<String, dynamic> j) => VUser(
        email: (j['email'] ?? '') as String,
        name: j['name'] as String?,
        userRole: j['userRole'] as String?,
        lastSyncedAt: _date(j['lastSyncedAt']),
      );
}

class VEmail {
  VEmail({
    required this.id,
    this.senderName,
    this.senderEmail,
    this.subject,
    this.snippet,
    this.category,
    this.priority,
    this.isRead = false,
    this.receivedAt,
    this.deadlineDetected,
  });

  final int id;
  final String? senderName;
  final String? senderEmail;
  final String? subject;
  final String? snippet;
  final String? category;
  final String? priority;
  final bool isRead;
  final DateTime? receivedAt;
  final DateTime? deadlineDetected;

  String get sender => senderName?.isNotEmpty == true ? senderName! : (senderEmail ?? 'Unknown');
  String get title => subject?.isNotEmpty == true ? subject! : '(no subject)';

  /// Rank used to order the priority bands: HIGH first.
  int get priorityRank => switch (priority) { 'HIGH' => 0, 'MEDIUM' => 1, _ => 2 };

  factory VEmail.fromJson(Map<String, dynamic> j) => VEmail(
        id: _int(j['id']),
        senderName: j['senderName'] as String?,
        senderEmail: j['senderEmail'] as String?,
        subject: j['subject'] as String?,
        snippet: (j['bodySnippet'] ?? j['snippet']) as String?,
        category: j['category'] as String?,
        priority: j['priority'] as String?,
        isRead: j['isRead'] == true,
        receivedAt: _date(j['receivedAt']),
        deadlineDetected: _date(j['deadlineDetected']),
      );
}

class DashboardSummary {
  DashboardSummary({
    required this.unreadCount,
    required this.upcomingDeadlines,
    required this.pendingActions,
    required this.categoryCounts,
  });

  final int unreadCount;
  final List<Map<String, dynamic>> upcomingDeadlines;
  final List<Map<String, dynamic>> pendingActions;
  final Map<String, int> categoryCounts;

  /// Deadlines already in the past — these drag the score hardest.
  int get overdueCount => upcomingDeadlines.where((d) {
        final due = _date(d['dueDate'] ?? d['deadline'] ?? d['deadlineDetected']);
        return due != null && due.isBefore(DateTime.now());
      }).length;

  /// Velocity Score, computed identically to the web client
  /// (pages/DashboardPageNew.tsx): start at 100, debit the three things
  /// that actually slow you down. Kept in sync deliberately — a score that
  /// differed between clients would be worse than no score.
  int get velocityScore {
    final raw = 100 -
        (unreadCount * 1.2).clamp(0, 45) -
        (pendingActions.length * 3).clamp(0, 25) -
        (overdueCount * 5).clamp(0, 20);
    return raw.clamp(0, 100).round();
  }

  String get verdict => switch (velocityScore) {
        >= 75 => 'Running clear',
        >= 45 => 'Some drag',
        _ => 'Backlog building',
      };

  factory DashboardSummary.fromJson(Map<String, dynamic> j) => DashboardSummary(
        unreadCount: _int(j['unreadCount']),
        upcomingDeadlines: ((j['upcomingDeadlines'] as List?) ?? const [])
            .whereType<Map<String, dynamic>>()
            .toList(),
        pendingActions:
            ((j['pendingActions'] as List?) ?? const []).whereType<Map<String, dynamic>>().toList(),
        categoryCounts: ((j['categoryCounts'] as Map?) ?? const {})
            .map((k, v) => MapEntry(k.toString(), _int(v))),
      );
}
