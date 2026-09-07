// Cortex category colors — CSS vars swap with html[data-theme].

export const CAT_COLORS: Record<string, { label: string; bg: string; text: string }> = {
  PLACEMENT:     { label: 'Opportunity',  bg: 'var(--cat-placement-bg)', text: 'var(--cat-placement-text)' },
  INTERNSHIP:    { label: 'Internship',   bg: 'var(--cat-internship-bg)', text: 'var(--cat-internship-text)' },
  ASSIGNMENT:    { label: 'Task',         bg: 'var(--cat-assignment-bg)', text: 'var(--cat-assignment-text)' },
  ATTENDANCE:    { label: 'Check-in',     bg: 'var(--cat-attendance-bg)', text: 'var(--cat-attendance-text)' },
  HACKATHON:     { label: 'Event',        bg: 'var(--cat-hackathon-bg)', text: 'var(--cat-hackathon-text)' },
  MEETING:       { label: 'Meeting',      bg: 'var(--cat-meeting-bg)', text: 'var(--cat-meeting-text)' },
  ANNOUNCEMENT:  { label: 'Update',       bg: 'var(--cat-announcement-bg)', text: 'var(--cat-announcement-text)' },
  RESEARCH:      { label: 'Research',     bg: 'var(--cat-research-bg)', text: 'var(--cat-research-text)' },
  FINANCE:       { label: 'Finance',      bg: 'var(--cat-finance-bg)', text: 'var(--cat-finance-text)' },
  PERSONAL:      { label: 'Personal',     bg: 'var(--cat-personal-bg)', text: 'var(--cat-personal-text)' },
  PROMOTIONAL:   { label: 'Promo',        bg: 'var(--cat-promotional-bg)', text: 'var(--cat-promotional-text)' },
  SPAM:          { label: 'Spam',         bg: 'var(--cat-spam-bg)', text: 'var(--cat-spam-text)' },
  UNCATEGORIZED: { label: 'Other',        bg: 'var(--cat-other-bg)', text: 'var(--cat-other-text)' },
};

export const CATEGORY_LABELS: Record<string, string> = {
  ASSIGNMENT:    'Tasks',
  ATTENDANCE:    'Check-ins',
  HACKATHON:     'Events',
  PLACEMENT:     'Opportunities',
  INTERNSHIP:    'Internships',
  MEETING:       'Meetings',
  ANNOUNCEMENT:  'Updates',
  RESEARCH:      'Research',
  FINANCE:       'Finance',
  PERSONAL:      'Personal',
  PROMOTIONAL:   'Promotions',
  SPAM:          'Spam',
  UNCATEGORIZED: 'Other',
};

/** Semantic score band colors for Cortex Score gauge. */
export function scoreToneFor(value: number | null, ready: boolean): string {
  if (!ready || value == null) return 'var(--color-text-muted)';
  if (value >= 80) return 'var(--color-success)';
  if (value >= 60) return '#84CC16';
  if (value >= 40) return 'var(--color-warning)';
  if (value >= 20) return '#FB923C';
  return 'var(--color-danger)';
}
