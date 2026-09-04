import axiosInstance from './axiosInstance';

export interface GeminiStatus {
  configured: boolean;
  mode?: 'gemini' | 'rules';
}

export const settingsApi = {
  getGeminiStatus: async (): Promise<GeminiStatus> => {
    const { data } = await axiosInstance.get<GeminiStatus>('/api/settings/gemini-status');
    return data;
  },
};
