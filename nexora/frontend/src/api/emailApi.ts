import axiosInstance from './axiosInstance';
import type { EmailPage, EmailReaction, GmailLabelCount, GmailSyncResult } from '../types/Email';

export interface SenderSummary {
  senderEmail: string;
  senderName: string | null;
  emailCount: number;
  latestSubject: string | null;
  latestReceivedAt: string | null;
}

export const emailApi = {
  getEmails: async (params: {
    category?: string;
    priority?: string;
    search?: string;
    view?: string;
    page?: number;
    size?: number;
  }): Promise<EmailPage> => {
    const { data } = await axiosInstance.get<EmailPage>('/api/emails', { params });
    return data;
  },

  getDrafts: async (params?: { search?: string; page?: number; size?: number }): Promise<EmailPage> => {
    const { data } = await axiosInstance.get<EmailPage>('/api/emails/drafts', { params });
    return data;
  },

  syncDrafts: async (): Promise<GmailSyncResult> => {
    const { data } = await axiosInstance.post<GmailSyncResult>('/api/emails/drafts/sync', null, {
      timeout: 60_000,
    });
    return data;
  },

  getArchived: async (params?: { search?: string; page?: number; size?: number }): Promise<EmailPage> => {
    const { data } = await axiosInstance.get<EmailPage>('/api/emails/archived', { params });
    return data;
  },

  getSyncStatus: async (): Promise<{
    connected: boolean;
    lastSyncedAt?: string;
    gmailCounts: Record<string, number>;
    localCounts: Record<string, number>;
    categoryGroups: Record<string, number>;
    unclassifiedInbox: number;
    inboxAligned: boolean;
    draftsAligned: boolean;
    secondaryComplete?: boolean;
    syncInProgress?: boolean;
    notes: string[];
    sampleInbox: Array<Record<string, unknown>>;
  }> => {
    const { data } = await axiosInstance.get('/api/emails/sync-status');
    return data;
  },

  getEmail: async (id: number) => {
    const { data } = await axiosInstance.get(`/api/emails/${id}`);
    return data;
  },

  syncEmails: async (): Promise<GmailSyncResult> => {
    const { data } = await axiosInstance.post<GmailSyncResult>('/api/emails/sync', null, {
      timeout: 60_000,
    });
    return data;
  },

  getGmailLabelCounts: async (): Promise<Record<string, GmailLabelCount>> => {
    const { data } = await axiosInstance.get<Record<string, GmailLabelCount>>('/api/emails/labels/counts');
    return data;
  },

  classifyInbox: async (options: { force?: boolean } = {}): Promise<{
    message: string;
    classified: number;
    groups?: Record<string, number>;
    forced?: boolean;
  }> => {
    const { data } = await axiosInstance.post('/api/emails/classify', null, {
      params: { force: options?.force ?? false },
    });
    return data;
  },

  getCategoryCounts: async (): Promise<Record<string, number>> => {
    const { data } = await axiosInstance.get('/api/emails/categories');
    return data;
  },

  markAllRead: async (): Promise<{ updated: number; message?: string }> => {
    const { data } = await axiosInstance.patch<{ updated: number; message?: string }>('/api/emails/read-all');
    return data;
  },

  bulk: async (
    ids: number[],
    action: 'READ' | 'UNREAD' | 'STAR' | 'UNSTAR' | 'ARCHIVE' | 'TRASH',
  ): Promise<{ updated: number; skipped?: number; action: string }> => {
    const { data } = await axiosInstance.post<{ updated: number; skipped?: number; action: string }>(
      '/api/emails/bulk',
      { ids, action },
    );
    return data;
  },

  markRead: async (id: number): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/read`);
  },

  markUnread: async (id: number): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/unread`);
  },

  setStarred: async (id: number, starred: boolean): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/star`, { starred });
  },

  archive: async (id: number): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/archive`);
  },

  moveToInbox: async (id: number): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/inbox`);
  },

  trash: async (id: number): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/trash`);
  },

  restore: async (id: number): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/restore`);
  },

  updateReaction: async (id: number, reaction: EmailReaction): Promise<void> => {
    await axiosInstance.patch(`/api/emails/${id}/reaction`, { reaction });
  },

  getSenderSummary: async (): Promise<SenderSummary[]> => {
    const { data } = await axiosInstance.get<SenderSummary[]>('/api/emails/by-sender');
    return data;
  },

  getEmailsFromSender: async (
    senderEmail: string,
    page = 0,
    size = 20,
  ): Promise<EmailPage> => {
    const { data } = await axiosInstance.get<EmailPage>(
      `/api/emails/sender/${encodeURIComponent(senderEmail)}`,
      { params: { page, size } },
    );
    return data;
  },

  getEmailThread: async (threadId: string): Promise<any[]> => {
    const { data } = await axiosInstance.get<any[]>(`/api/emails/thread/${threadId}`);
    return data;
  },

  getShared: async (params?: { page?: number; size?: number }): Promise<EmailPage> => {
    const { data } = await axiosInstance.get<EmailPage>('/api/emails/shared', { params });
    return data;
  },
};
