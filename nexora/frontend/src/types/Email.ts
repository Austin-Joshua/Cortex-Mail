export type EmailCategory =
  | 'ASSIGNMENT' | 'ATTENDANCE' | 'HACKATHON' | 'PLACEMENT' | 'INTERNSHIP'
  | 'MEETING' | 'ANNOUNCEMENT' | 'RESEARCH' | 'FINANCE' | 'PERSONAL'
  | 'PROMOTIONAL' | 'SPAM' | 'UNCATEGORIZED';

export type Priority = 'HIGH' | 'MEDIUM' | 'LOW';

export type EmailReaction = 'NONE' | 'DONE' | 'IMPORTANT' | 'LATER' | 'IGNORE' | 'SNOOZED';

export type ActionType = 'REGISTER' | 'REPLY' | 'SUBMIT' | 'UPLOAD' | 'REVIEW' | 'ATTEND' | 'OTHER';

export interface ActionItem {
  id: number;
  actionType: ActionType;
  actionDescription: string;
  deadline?: string;
  isCompleted: boolean;
}

export interface GmailLabelCount {
  id: string;
  name: string;
  type?: string;
  messagesTotal?: number;
  messagesUnread?: number;
  threadsTotal?: number;
  threadsUnread?: number;
}

export interface GmailSyncResult {
  message: string;
  newCount: number;
  updatedCount: number;
  inboxMessagesProcessed: number;
  labelCounts: Record<string, GmailLabelCount>;
}

export interface Email {
  id: number;
  gmailMessageId: string;
  gmailThreadId?: string;
  senderName?: string;
  senderEmail: string;
  subject?: string;
  bodySnippet?: string;
  bodyFull?: string;
  receivedAt?: string;
  isRead: boolean;
  hasAttachments: boolean;
  gmailLabelIds?: string;
  recipientTo?: string;
  recipientCc?: string;
  isStarred?: boolean;
  isImportant?: boolean;
  inInbox?: boolean;
  isDraft?: boolean;
  isArchived?: boolean;
  sizeEstimate?: number;
  category: EmailCategory;
  priority: Priority;
  reaction?: EmailReaction;
  aiSummary?: string;
  aiActionItems?: string;
  deadlineDetected?: string;
  isDeadlineAddedToCalendar: boolean;
  actions?: ActionItem[];
  createdAt?: string;
}

export interface EmailPage {
  content: Email[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
