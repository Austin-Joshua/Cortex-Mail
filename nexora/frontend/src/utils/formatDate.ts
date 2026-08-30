import { format, formatDistanceToNow, parseISO, isValid } from 'date-fns';

function formatDate(dateStr?: string, fmt = 'MMM d, yyyy'): string {
  if (!dateStr) return '—';
  try {
    const d = parseISO(dateStr);
    return isValid(d) ? format(d, fmt) : '—';
  } catch {
    return '—';
  }
}

export function formatRelative(dateStr?: string): string {
  if (!dateStr) return '';
  try {
    const d = parseISO(dateStr);
    return isValid(d) ? formatDistanceToNow(d, { addSuffix: true }) : '';
  } catch {
    return '';
  }
}

export function formatDateTime(dateStr?: string): string {
  return formatDate(dateStr, 'MMM d, yyyy · h:mm a');
}
