import type { EmailCategory } from '../types/Email';
import type { UserRole } from '../types/User';

export type InboxDivisionKey = EmailCategory | 'SENDERS';

export interface InboxDivision {
  key: InboxDivisionKey;
  label: string;
}

/** Preferred division order per account type — tabs with zero mail are hidden. */
const ROLE_DIVISIONS: Record<UserRole, InboxDivision[]> = {
  STUDENT: [
    { key: 'ASSIGNMENT', label: 'Assignments' },
    { key: 'HACKATHON', label: 'Hackathons' },
    { key: 'PLACEMENT', label: 'Placement' },
    { key: 'INTERNSHIP', label: 'Internships' },
    { key: 'ATTENDANCE', label: 'Attendance' },
    { key: 'MEETING', label: 'Meetings' },
    { key: 'ANNOUNCEMENT', label: 'Announcements' },
    { key: 'RESEARCH', label: 'Research' },
    { key: 'FINANCE', label: 'Finance' },
    { key: 'PERSONAL', label: 'Personal' },
    { key: 'PROMOTIONAL', label: 'Promotions' },
  ],
  PROFESSOR: [
    { key: 'RESEARCH', label: 'Research' },
    { key: 'MEETING', label: 'Meetings' },
    { key: 'ANNOUNCEMENT', label: 'Announcements' },
    { key: 'ASSIGNMENT', label: 'Student work' },
    { key: 'ATTENDANCE', label: 'Attendance' },
    { key: 'PERSONAL', label: 'Personal' },
    { key: 'FINANCE', label: 'Finance' },
    { key: 'PROMOTIONAL', label: 'Promotions' },
  ],
  HR_PROFESSIONAL: [
    { key: 'PLACEMENT', label: 'Recruiting' },
    { key: 'INTERNSHIP', label: 'Internships' },
    { key: 'MEETING', label: 'Meetings' },
    { key: 'ANNOUNCEMENT', label: 'Announcements' },
    { key: 'PERSONAL', label: 'Personal' },
    { key: 'FINANCE', label: 'Finance' },
    { key: 'PROMOTIONAL', label: 'Promotions' },
  ],
  IT_EMPLOYEE: [
    { key: 'MEETING', label: 'Meetings' },
    { key: 'ANNOUNCEMENT', label: 'Alerts' },
    { key: 'FINANCE', label: 'Finance' },
    { key: 'PERSONAL', label: 'Personal' },
    { key: 'PROMOTIONAL', label: 'Promotions' },
  ],
  MANAGER: [
    { key: 'MEETING', label: 'Meetings' },
    { key: 'ANNOUNCEMENT', label: 'Updates' },
    { key: 'FINANCE', label: 'Finance' },
    { key: 'PLACEMENT', label: 'Hiring' },
    { key: 'PERSONAL', label: 'Personal' },
    { key: 'PROMOTIONAL', label: 'Promotions' },
  ],
  FREELANCER: [
    { key: 'FINANCE', label: 'Invoices' },
    { key: 'MEETING', label: 'Meetings' },
    { key: 'ANNOUNCEMENT', label: 'Clients' },
    { key: 'PERSONAL', label: 'Personal' },
    { key: 'PROMOTIONAL', label: 'Promotions' },
  ],
};

const OTHER_DIVISION: InboxDivision = { key: 'UNCATEGORIZED', label: 'Other' };

export function getInboxDivisions(role: UserRole | undefined): InboxDivision[] {
  return ROLE_DIVISIONS[role ?? 'STUDENT'] ?? ROLE_DIVISIONS.STUDENT;
}

/** Show Senders always; show category tabs only when mail exists in that division. */
export function getVisibleInboxDivisions(
  role: UserRole | undefined,
  counts: Record<string, number>,
): InboxDivision[] {
  const base = getInboxDivisions(role).filter((d) => (counts[d.key] ?? 0) > 0);
  const extras: InboxDivision[] = [{ key: 'SENDERS', label: 'Senders' }];
  if ((counts.UNCATEGORIZED ?? 0) > 0 && !base.some((d) => d.key === 'UNCATEGORIZED')) {
    extras.push(OTHER_DIVISION);
  }
  return [...extras, ...base];
}
