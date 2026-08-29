import axiosInstance from './axiosInstance';
import type { GmailLabelCount } from '../types/Email';

export interface DashboardSummary {
  priorityEmails: any[];
  upcomingDeadlines: any[];
  pendingActions: any[];
  unreadCount: number;
  categoryCounts: Record<string, number>;
  gmailLabelCounts?: Record<string, GmailLabelCount>;
  todaysMeetings: any[];
}

export interface VolumeDataPoint {
  date: string;
  count: number;
}

export const dashboardApi = {
  getSummary: async (): Promise<DashboardSummary> => {
    const { data } = await axiosInstance.get<DashboardSummary>('/api/dashboard/summary');
    return data;
  },

  getEmailVolume: async (days: number = 7): Promise<VolumeDataPoint[]> => {
    const { data } = await axiosInstance.get<VolumeDataPoint[]>(`/api/analytics/volume?days=${days}`);
    return data;
  },
};
