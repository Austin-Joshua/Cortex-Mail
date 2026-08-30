export type EmailCategory =
  | 'ASSIGNMENT' | 'ATTENDANCE' | 'HACKATHON' | 'PLACEMENT' | 'INTERNSHIP'
  | 'MEETING' | 'ANNOUNCEMENT' | 'RESEARCH' | 'FINANCE' | 'PERSONAL'
  | 'PROMOTIONAL' | 'SPAM' | 'UNCATEGORIZED';

export type Priority = 'HIGH' | 'MEDIUM' | 'LOW';

export type EmailReaction = 'NONE' | 'DONE' | 'IMPORTANT' | 'LATER' | 'IGNORE' | 'SNOOZED';

type ActionType = 'REGISTER' | 'REPLY' | 'SUBMIT' | 'UPLOAD' | 'REVIEW' | 'ATTEND' | 'OTHER';

interface ActionItem {
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

interface EmailAttachment {
  id?: number;
  gmailAttachmentId?: string;
  filename?: string;
  mimeType?: string;
  sizeBytes?: number;
  contentId?: string;
  isInline?: boolean;
}

export interface GmailSyncResult {
  message: string;
  newCount: number;
  updatedCount: number;
  inboxMessagesProcessed: number;
  labelCounts: Record<string, GmailLabelCount>;
  syncMode?: 'FULL' | 'INCREMENTAL' | 'SKIPPED' | 'FAST_FIRST' | 'STARTED';
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
  bodyHtml?: string;
  receivedAt?: string;
  isRead: boolean;
  hasAttachments: boolean;
  attachments?: EmailAttachment[];
  gmailLabelIds?: string;
  recipientTo?: string;
  recipientCc?: string;
  isStarred?: boolean;
  isImportant?: boolean;
  inInbox?: boolean;
  isDraft?: boolean;
  isArchived?: boolean;
  isTrash?: boolean;
  isSpam?: boolean;
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
  last?: boolean;
}
