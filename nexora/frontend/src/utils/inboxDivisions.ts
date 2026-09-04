import type { EmailCategory } from '../types/Email';
import type { GmailLabelCount } from '../types/Email';

export type GmailMailboxView =
  | 'UNREAD'
  | 'STARRED'
  | 'IMPORTANT'
  | 'PRIMARY'
  | 'PROMOTIONS'
  | 'SOCIAL'
  | 'UPDATES'
  | 'FORUMS';

export type InboxDivisionKey = EmailCategory | 'SENDERS' | GmailMailboxView;

export interface InboxDivision {
  key: InboxDivisionKey;
  label: string;
  kind: 'gmail' | 'cortex' | 'people';
}

type GmailInboxDivision = InboxDivision & { key: GmailMailboxView; kind: 'gmail' };

export const GMAIL_VIEWS: GmailInboxDivision[] = [
  { key: 'UNREAD', label: 'Unread', kind: 'gmail' },
  { key: 'STARRED', label: 'Starred', kind: 'gmail' },
  { key: 'IMPORTANT', label: 'Flagged', kind: 'gmail' },
  { key: 'PRIMARY', label: 'Primary', kind: 'gmail' },
  { key: 'PROMOTIONS', label: 'Promotions', kind: 'gmail' },
  { key: 'SOCIAL', label: 'Social', kind: 'gmail' },
  { key: 'UPDATES', label: 'Updates', kind: 'gmail' },
  { key: 'FORUMS', label: 'Forums', kind: 'gmail' },
];

const CORTEX_DIVISIONS: InboxDivision[] = [
  { key: 'MEETING', label: 'Meetings', kind: 'cortex' },
  { key: 'ASSIGNMENT', label: 'Tasks', kind: 'cortex' },
  { key: 'ANNOUNCEMENT', label: 'Notices', kind: 'cortex' },
  { key: 'FINANCE', label: 'Finance', kind: 'cortex' },
  { key: 'PLACEMENT', label: 'Opportunities', kind: 'cortex' },
  { key: 'INTERNSHIP', label: 'Internships', kind: 'cortex' },
  { key: 'HACKATHON', label: 'Events', kind: 'cortex' },
  { key: 'RESEARCH', label: 'Research', kind: 'cortex' },
  { key: 'ATTENDANCE', label: 'Check-ins', kind: 'cortex' },
  { key: 'PERSONAL', label: 'Personal', kind: 'cortex' },
  { key: 'PROMOTIONAL', label: 'Promo', kind: 'cortex' },
];

const GMAIL_LABEL_FOR_VIEW: Record<GmailMailboxView, string> = {
  UNREAD: 'INBOX',
  STARRED: 'STARRED',
  IMPORTANT: 'IMPORTANT',
  PRIMARY: 'CATEGORY_PERSONAL',
  PROMOTIONS: 'CATEGORY_PROMOTIONS',
  SOCIAL: 'CATEGORY_SOCIAL',
  UPDATES: 'CATEGORY_UPDATES',
  FORUMS: 'CATEGORY_FORUMS',
};

export const GMAIL_VIEW_KEYS = new Set<string>(GMAIL_VIEWS.map((v) => v.key));

export function isGmailMailboxView(key: string | null | undefined): key is GmailMailboxView {
  return !!key && GMAIL_VIEW_KEYS.has(key);
}

export function gmailViewCount(
  key: GmailMailboxView,
  labels?: Record<string, GmailLabelCount>,
): number {
  if (!labels) return 0;
  if (key === 'UNREAD') {
    return labels.INBOX?.messagesUnread ?? labels.UNREAD?.messagesTotal ?? 0;
  }
  const label = labels[GMAIL_LABEL_FOR_VIEW[key]];
  return label?.messagesTotal ?? 0;
}

/** Gmail tabs that have mail, plus Cortex tabs that this mailbox actually filled. */
export function getVisibleInboxDivisions(
  _role: string | undefined,
  counts: Record<string, number>,
  labels?: Record<string, GmailLabelCount>,
): InboxDivision[] {
  const gmailTabs = GMAIL_VIEWS.filter((tab) => {
    if (tab.key === 'UNREAD' || tab.key === 'STARRED' || tab.key === 'PRIMARY') return true;
    return gmailViewCount(tab.key, labels) > 0;
  });

  const cortex = CORTEX_DIVISIONS.filter((d) => (counts[d.key] ?? 0) > 0);
  const extras: InboxDivision[] = [{ key: 'SENDERS', label: 'Senders', kind: 'people' }];
  if ((counts.UNCATEGORIZED ?? 0) > 0) {
    extras.push({ key: 'UNCATEGORIZED', label: 'Other', kind: 'cortex' });
  }
  if ((counts.SPAM ?? 0) > 0) {
    extras.push({ key: 'SPAM', label: 'Spam', kind: 'cortex' });
  }
  return [...gmailTabs, ...extras, ...cortex];
}
