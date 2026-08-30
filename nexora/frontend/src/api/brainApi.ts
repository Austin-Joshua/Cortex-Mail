import axiosInstance from './axiosInstance';
import type { BrainQueryResponse, BrainConversation } from '../types/Brain';

export const brainApi = {
  query: async (query: string): Promise<BrainQueryResponse> => {
    const { data } = await axiosInstance.post<BrainQueryResponse>(
      '/api/brain/query',
      { query },
      { timeout: 120_000 },
    );
    return data;
  },

  getHistory: async (): Promise<BrainConversation[]> => {
    const { data } = await axiosInstance.get<BrainConversation[]>('/api/brain/history');
    return data;
  },
};
