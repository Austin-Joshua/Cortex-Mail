import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../models/models.dart';

class ApiException implements Exception {
  ApiException(this.statusCode, this.message);
  final int statusCode;
  final String message;
  @override
  String toString() => 'ApiException($statusCode): $message';
}

/// Talks to the same Spring Boot backend as the web client. Endpoint paths
/// mirror frontend/src/api/* exactly so the two stay in step.
///
/// The base URL is injected at build time and never hardcoded:
///   flutter build apk --dart-define=API_BASE_URL=https://your-backend
/// No API keys live in this client — AI keys stay server-side, and the only
/// credential here is the user's own JWT.
class VelocityApi {
  VelocityApi({http.Client? client, String? baseUrl})
      : _http = client ?? http.Client(),
        baseUrl = baseUrl ??
            const String.fromEnvironment(
              'API_BASE_URL',
              defaultValue: 'http://10.0.2.2:8080',
            );

  final http.Client _http;
  final String baseUrl;

  static const _tokenKey = 'velocity_jwt';
  String? _token;

  Future<void> loadToken() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString(_tokenKey);
  }

  Future<void> setToken(String? token) async {
    _token = token;
    final prefs = await SharedPreferences.getInstance();
    if (token == null) {
      await prefs.remove(_tokenKey);
    } else {
      await prefs.setString(_tokenKey, token);
    }
  }

  bool get isAuthenticated => _token != null && _token!.isNotEmpty;

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (_token != null) 'Authorization': 'Bearer $_token',
      };

  Uri _uri(String path, [Map<String, dynamic>? query]) {
    final uri = Uri.parse('$baseUrl$path');
    if (query == null || query.isEmpty) return uri;
    final params = <String, String>{};
    query.forEach((k, v) {
      if (v != null) params[k] = '$v';
    });
    return uri.replace(queryParameters: params);
  }

  Future<dynamic> _get(String path, [Map<String, dynamic>? query]) async {
    final res = await _http.get(_uri(path, query), headers: _headers);
    return _decode(res);
  }

  Future<dynamic> _post(String path, [Object? body]) async {
    final res = await _http.post(
      _uri(path),
      headers: _headers,
      body: body == null ? null : jsonEncode(body),
    );
    return _decode(res);
  }

  dynamic _decode(http.Response res) {
    if (res.statusCode >= 200 && res.statusCode < 300) {
      if (res.body.isEmpty) return null;
      return jsonDecode(res.body);
    }
    String message = 'Request failed';
    try {
      final body = jsonDecode(res.body);
      if (body is Map && body['message'] is String) message = body['message'];
    } catch (_) {/* non-JSON error body */}
    throw ApiException(res.statusCode, message);
  }

  /// Controllers added alongside drafts/templates/priority wrap payloads in
  /// an ApiResponse envelope; the older ones return bare bodies.
  dynamic _unwrap(dynamic body) {
    if (body is Map<String, dynamic> && body.containsKey('success') && body.containsKey('data')) {
      return body['data'];
    }
    return body;
  }

  // ------------------------------------------------------------------ auth

  Future<VUser> me() async => VUser.fromJson(await _get('/api/auth/me') as Map<String, dynamic>);

  Future<void> logout() => setToken(null);

  // -------------------------------------------------------------- dashboard

  Future<DashboardSummary> dashboardSummary() async =>
      DashboardSummary.fromJson(await _get('/api/dashboard/summary') as Map<String, dynamic>);

  // ----------------------------------------------------------------- email

  Future<List<VEmail>> emails({
    String? category,
    String? priority,
    String? search,
    int page = 0,
    int size = 20,
  }) async {
    final body = await _get('/api/emails', {
      if (category != null) 'category': category,
      if (priority != null) 'priority': priority,
      if (search != null && search.isNotEmpty) 'search': search,
      'page': page,
      'size': size,
    });
    final content = (body as Map<String, dynamic>)['content'] as List? ?? const [];
    return content.map((e) => VEmail.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Map<String, int>> categoryCounts() async {
    final body = await _get('/api/emails/categories') as Map<String, dynamic>;
    return body.map((k, v) => MapEntry(k, (v as num).toInt()));
  }

  Future<void> sync() => _post('/api/emails/sync');

  Future<void> markRead(int id) => _post('/api/emails/$id/read');

  // -------------------------------------------------------------- priority

  Future<List<VEmail>> priorityEmails({int limit = 20}) async {
    final body = _unwrap(await _get('/api/priority', {'limit': limit}));
    return (body as List? ?? const [])
        .map((e) => VEmail.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // ----------------------------------------------------------------- brain

  Future<String> askBrain(String question) async {
    final body = await _post('/api/brain/query', {'query': question});
    if (body is Map<String, dynamic>) {
      return (body['answer'] ?? body['response'] ?? body['text'] ?? '').toString();
    }
    return body.toString();
  }

  void close() => _http.close();
}
