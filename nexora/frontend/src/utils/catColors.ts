// Cortex category colors — expressive badges (Gmail layer stays neutral elsewhere).

export const CAT_COLORS: Record<string, { label: string; bg: string; text: string }> = {
  PLACEMENT:     { label: 'Placement',    bg: '#24173F', text: '#A78BFA' },
  INTERNSHIP:    { label: 'Internship',   bg: '#1D1D45', text: '#818CF8' },
  ASSIGNMENT:    { label: 'Assignment',   bg: '#14263D', text: '#60A5FA' },
  ATTENDANCE:    { label: 'Attendance',   bg: '#102D2C', text: '#2DD4BF' },
  HACKATHON:     { label: 'Hackathon',    bg: '#351B31', text: '#F472B6' },
  MEETING:       { label: 'Meeting',      bg: '#102D2C', text: '#2DD4BF' },
  ANNOUNCEMENT:  { label: 'Announcement', bg: '#24173F', text: '#C4B5FD' },
  RESEARCH:      { label: 'Research',     bg: '#122D20', text: '#4ADE80' },
  FINANCE:       { label: 'Finance',      bg: '#332A12', text: '#FBBF24' },
  PERSONAL:      { label: 'Personal',     bg: '#14263D', text: '#60A5FA' },
  PROMOTIONAL:   { label: 'Promo',        bg: '#202734', text: '#94A3B8' },
  SPAM:          { label: 'Spam',         bg: '#35191D', text: '#F87171' },
  UNCATEGORIZED: { label: 'Other',        bg: '#202734', text: '#9AA6B2' },
};

export const CATEGORY_LABELS: Record<string, string> = {
  ASSIGNMENT:    'Assignment',
  ATTENDANCE:    'Attendance',
  HACKATHON:     'Hackathon',
  PLACEMENT:     'Placement',
  INTERNSHIP:    'Internship',
  MEETING:       'Meeting',
  ANNOUNCEMENT:  'Announcement',
  RESEARCH:      'Research',
  FINANCE:       'Finance',
  PERSONAL:      'Personal',
  PROMOTIONAL:   'Promotional',
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
