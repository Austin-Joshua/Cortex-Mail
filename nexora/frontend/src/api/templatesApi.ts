import axiosInstance from './axiosInstance';
import { unwrapApiData, unwrapApiList } from '../utils/unwrapApiList';

export interface EmailTemplate {
  id: number;
  name: string;
  subject?: string;
  body?: string;
  htmlBody?: string;
  category?: string;
  usageCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export const templatesApi = {
  list: async (): Promise<EmailTemplate[]> => {
    const { data } = await axiosInstance.get<{ data?: EmailTemplate[] } | EmailTemplate[]>('/api/templates');
    return unwrapApiList(data);
  },

  create: async (payload: { name: string; subject?: string; body?: string; category?: string }): Promise<EmailTemplate> => {
    const { data } = await axiosInstance.post<{ data?: EmailTemplate } | EmailTemplate>('/api/templates', payload);
    return unwrapApiData(data);
  },

  update: async (id: number, payload: Partial<EmailTemplate>): Promise<EmailTemplate> => {
    const { data } = await axiosInstance.put<{ data?: EmailTemplate } | EmailTemplate>(`/api/templates/${id}`, payload);
    return unwrapApiData(data);
  },

  remove: async (id: number): Promise<void> => {
    await axiosInstance.delete(`/api/templates/${id}`);
  },

  recordUse: async (id: number): Promise<EmailTemplate> => {
    const { data } = await axiosInstance.post<{ data?: EmailTemplate } | EmailTemplate>(`/api/templates/${id}/use`);
    return unwrapApiData(data);
  },
};
