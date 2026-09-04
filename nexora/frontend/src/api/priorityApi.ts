import axiosInstance from './axiosInstance';
import type { Email } from '../types/Email';
import { unwrapApiData, unwrapApiList } from '../utils/unwrapApiList';

export const priorityApi = {
  getPriority: async (limit = 50): Promise<Email[]> => {
    const { data } = await axiosInstance.get<{ data?: Email[] } | Email[]>('/api/priority', {
      params: { limit },
    });
    return unwrapApiList(data);
  },

  getSuggestions: async (): Promise<Email[]> => {
    const { data } = await axiosInstance.get<{ data?: Email[] } | Email[]>('/api/priority/suggestions');
    return unwrapApiList(data);
  },

  flag: async (emailId: number): Promise<Email> => {
    const { data } = await axiosInstance.post<{ data?: Email } | Email>(`/api/priority/${emailId}/flag`);
    return unwrapApiData(data);
  },

  unflag: async (emailId: number): Promise<Email> => {
    const { data } = await axiosInstance.post<{ data?: Email } | Email>(`/api/priority/${emailId}/unflag`);
    return unwrapApiData(data);
  },
};
