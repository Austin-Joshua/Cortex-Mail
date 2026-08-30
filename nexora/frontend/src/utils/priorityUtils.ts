import type { Priority } from '../types/Email';

export const PRIORITY_CLASS: Record<Priority, string> = {
  HIGH:   'badge-priority-high priority-high',
  MEDIUM: 'badge-priority-medium priority-medium',
  LOW:    'badge-priority-low priority-low',
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  HIGH:   'HIGH',
  MEDIUM: 'MEDIUM',
  LOW:    'LOW',
};

export function getPriorityClass(priority: Priority): string {
  return PRIORITY_CLASS[priority] || 'priority-medium';
}
