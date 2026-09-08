import axiosInstance from './axiosInstance';

export interface EmailActionItem {
  id: number;
  emailId?: number;
  emailSubject?: string;
  actionType?: string;
  actionDescription?: string;
  deadline?: string;
  isCompleted?: boolean;
  snoozedUntil?: string;
}

export const emailActionsApi = {
  pending: async (): Promise<EmailActionItem[]> => {
    const { data } = await axiosInstance.get<EmailActionItem[]>('/api/email-actions/pending');
    return data ?? [];
  },

  complete: async (id: number): Promise<void> => {
    await axiosInstance.patch(`/api/email-actions/${id}/complete`);
  },

  snooze: async (id: number, hours = 24): Promise<EmailActionItem> => {
    const { data } = await axiosInstance.post<EmailActionItem>(
      `/api/email-actions/${id}/snooze`,
      null,
      { params: { hours } },
    );
    return data;
  },
};
