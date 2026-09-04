/** Normalize `{ data: T[] }` or bare `T[]` API payloads. */
export function unwrapApiList<T>(payload: { data?: T[] } | T[] | null | undefined): T[] {
  if (Array.isArray(payload)) return payload;
  if (payload && Array.isArray(payload.data)) return payload.data;
  return [];
}

/** Normalize `{ data: T }` envelopes used by drafts/templates/priority writes. */
export function unwrapApiData<T>(payload: { data?: T } | T | null | undefined): T {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    const nested = (payload as { data?: T }).data;
    if (nested !== undefined) return nested;
  }
  return payload as T;
}

