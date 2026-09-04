import axiosInstance from './axiosInstance';
import { unwrapApiData, unwrapApiList } from '../utils/unwrapApiList';

export interface CortexDraft {
  id: number;
  to?: string;
  cc?: string;
  bcc?: string;
  subject?: string;
  body?: string;
  htmlBody?: string;
  scheduledSendTime?: number;
  draftStatus?: string;
  createdAt?: string;
  updatedAt?: string;
}

export const draftsApi = {
  list: async (): Promise<CortexDraft[]> => {
    const { data } = await axiosInstance.get<{ data?: CortexDraft[] } | CortexDraft[]>('/api/drafts');
    return unwrapApiList(data);
  },

  create: async (payload: Partial<CortexDraft>): Promise<CortexDraft> => {
    const { data } = await axiosInstance.post<{ data?: CortexDraft } | CortexDraft>('/api/drafts', payload);
    return unwrapApiData(data);
  },

  update: async (id: number, payload: Partial<CortexDraft>): Promise<CortexDraft> => {
    const { data } = await axiosInstance.put<{ data?: CortexDraft } | CortexDraft>(`/api/drafts/${id}`, payload);
    return unwrapApiData(data);
  },

  remove: async (id: number): Promise<void> => {
    await axiosInstance.delete(`/api/drafts/${id}`);
  },
};
