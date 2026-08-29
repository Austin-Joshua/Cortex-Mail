// Category colors — red, green, orange only (+ grey for neutral).
// Used for mail category dots and tags.

export const CAT_COLORS: Record<string, { label: string; color: string }> = {
  ASSIGNMENT:    { label: 'Assignment',    color: '#DC2626' },  // red — urgent
  ATTENDANCE:    { label: 'Attendance',    color: '#DC2626' },  // red
  SPAM:          { label: 'Spam',          color: '#DC2626' },  // red
  HACKATHON:     { label: 'Hackathon',     color: '#EA580C' },  // orange
  MEETING:       { label: 'Meeting',       color: '#EA580C' },  // orange — calendar
  INTERNSHIP:    { label: 'Internship',    color: '#EA580C' },  // orange
  ANNOUNCEMENT:  { label: 'Announcement',  color: '#EA580C' },  // orange
  PLACEMENT:     { label: 'Placement',     color: '#16A34A' },  // green
  FINANCE:       { label: 'Finance',       color: '#16A34A' },  // green
  RESEARCH:      { label: 'Research',      color: '#16A34A' },  // green
  PERSONAL:      { label: 'Personal',      color: '#6B7280' },  // neutral grey
  PROMOTIONAL:   { label: 'Promo',         color: '#6B7280' },
  UNCATEGORIZED: { label: 'Other',         color: '#6B7280' },
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
