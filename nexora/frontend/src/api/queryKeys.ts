/** Shared React Query keys — keep shapes consistent so invalidation shares cache. */
export const queryKeys = {
  emails: ['emails'] as const,
  emailInbox: (view: string, search: string) => ['emails', 'inbox', view, search] as const,
  emailList: (params: unknown) => ['emails', params] as const,
  emailPriority: ['emails', 'dashboard-priority'] as const,
  emailDetail: (id: number | string) => ['email', id] as const,
  emailThread: (threadId: string | undefined) => ['email-thread', threadId] as const,
  emailDrafts: (search = '') => ['email-drafts', search] as const,
  emailArchived: (search = '') => ['email-archived', search] as const,
  emailCategories: ['email-categories'] as const,
  emailVolume: (days = 7) => ['email-volume', days] as const,
  syncStatus: ['sync-status'] as const,
  gmailLabelCounts: ['gmail-label-counts'] as const,
  dashboardSummary: ['dashboard-summary'] as const,
  senders: ['senders'] as const,
  senderEmails: (email: string | undefined) => ['sender-emails', email] as const,
  analyticsEmails: ['analytics-emails'] as const,
  brainHistory: ['brain-history'] as const,
  notifications: ['notifications'] as const,
  notificationsPage: ['notifications-page'] as const,
  notificationsUnread: ['notifications-unread-count'] as const,
};

export const SYNC_INVALIDATION_KEYS = [
  queryKeys.emails,
  queryKeys.emailCategories,
  queryKeys.gmailLabelCounts,
  queryKeys.emailDrafts(),
  queryKeys.emailArchived(),
  queryKeys.dashboardSummary,
  queryKeys.syncStatus,
] as const;
