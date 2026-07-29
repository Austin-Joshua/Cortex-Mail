// Category color + label system — used by CategoryTag and throughout the app.
//
// A warm-forward categorical set: brass through rust, with a jade, a teal and
// one dusty blue kept in for separation. Deliberately holds no indigo or
// violet, which read as foreign against the gold/ember ground. Each hue is
// mid-tone so it carries on both the light paper and the dark oxblood.

export const CAT_COLORS: Record<string, { label: string; color: string }> = {
  ASSIGNMENT:    { label: 'Assignment',    color: '#C8912B' },  // brass
  HACKATHON:     { label: 'Hackathon',     color: '#E0703A' },  // burnt orange
  PLACEMENT:     { label: 'Placement',     color: '#3E9E74' },  // jade
  MEETING:       { label: 'Meeting',       color: '#B5506B' },  // wine rose
  ATTENDANCE:    { label: 'Attendance',    color: '#D0453F' },  // red clay
  ANNOUNCEMENT:  { label: 'Announcement',  color: '#D9A441' },  // gold
  PROMOTIONAL:   { label: 'Promo',         color: '#94837A' },  // warm grey
  INTERNSHIP:    { label: 'Internship',    color: '#3E9B9B' },  // teal
  RESEARCH:      { label: 'Research',      color: '#6C86AE' },  // dusty blue
  FINANCE:       { label: 'Finance',       color: '#6E9E45' },  // olive
  PERSONAL:      { label: 'Personal',      color: '#D4788E' },  // dusty rose
  SPAM:          { label: 'Spam',          color: '#9E3B36' },  // rust
  UNCATEGORIZED: { label: 'Other',         color: '#94837A' },  // warm grey
};

// Legacy label map kept for backward-compat with existing pages
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
