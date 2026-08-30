/** Normalize `{ data: T[] }` or bare `T[]` API payloads. */
export function unwrapApiList<T>(payload: { data?: T[] } | T[] | null | undefined): T[] {
  if (Array.isArray(payload)) return payload;
  if (payload && Array.isArray(payload.data)) return payload.data;
  return [];
}
