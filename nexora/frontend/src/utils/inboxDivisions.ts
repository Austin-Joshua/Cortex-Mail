import type { EmailCategory } from '../types/Email';

export type InboxDivisionKey = EmailCategory | 'SENDERS';

export interface InboxDivision {
  key: InboxDivisionKey;
  label: string;
}

/** Universal inbox divisions — same for every account; empty tabs stay hidden. */
const UNIVERSAL_DIVISIONS: InboxDivision[] = [
  { key: 'MEETING', label: 'Meetings' },
  { key: 'ASSIGNMENT', label: 'Tasks' },
  { key: 'ANNOUNCEMENT', label: 'Updates' },
  { key: 'FINANCE', label: 'Finance' },
  { key: 'PLACEMENT', label: 'Opportunities' },
  { key: 'INTERNSHIP', label: 'Internships' },
  { key: 'HACKATHON', label: 'Events' },
  { key: 'RESEARCH', label: 'Research' },
  { key: 'ATTENDANCE', label: 'Check-ins' },
  { key: 'PERSONAL', label: 'Personal' },
  { key: 'PROMOTIONAL', label: 'Promotions' },
];

const OTHER_DIVISION: InboxDivision = { key: 'UNCATEGORIZED', label: 'Other' };

/** Show Senders always; show category tabs only when mail exists in that division. */
export function getVisibleInboxDivisions(
  _role: string | undefined,
  counts: Record<string, number>,
): InboxDivision[] {
  const base = UNIVERSAL_DIVISIONS.filter((d) => (counts[d.key] ?? 0) > 0);
  const extras: InboxDivision[] = [{ key: 'SENDERS', label: 'Senders' }];
  if ((counts.UNCATEGORIZED ?? 0) > 0 && !base.some((d) => d.key === 'UNCATEGORIZED')) {
    extras.push(OTHER_DIVISION);
  }
  if ((counts.SPAM ?? 0) > 0) {
    extras.push({ key: 'SPAM', label: 'Spam' });
  }
  return [...extras, ...base];
}
