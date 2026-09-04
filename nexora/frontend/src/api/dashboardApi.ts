import axiosInstance from './axiosInstance';
import type { GmailLabelCount } from '../types/Email';

interface CortexScoreFactor {
  key: string;
  label: string;
  points: number;
  detail: string;
}

interface CortexScore {
  score?: number | null;
  band: string;
  factors: CortexScoreFactor[];
  ready?: boolean;
  statusMessage?: string;
  nextAction?: string;
  inboxUnread?: number | null;
  overdueCount?: number | null;
  storedCount?: number | null;
}

export interface DashboardSummary {
  priorityEmails: any[];
  upcomingDeadlines: any[];
  pendingActions: any[];
  unreadCount: number;
  storedEmailCount?: number;
  categoryCounts: Record<string, number>;
  gmailLabelCounts?: Record<string, GmailLabelCount>;
  todaysMeetings: any[];
  cortexScore?: CortexScore;
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
