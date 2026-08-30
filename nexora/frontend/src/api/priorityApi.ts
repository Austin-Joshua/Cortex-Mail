import axiosInstance from './axiosInstance';
import type { Email } from '../types/Email';
import { unwrapApiList } from '../utils/unwrapApiList';

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
};
