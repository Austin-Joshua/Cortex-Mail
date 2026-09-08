import axiosInstance from './axiosInstance';
import type { AuthResponse, UserRole } from '../types/User';

// Resolve backend ORIGIN (no /api, no trailing slash)
const RAW_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const BACKEND_ORIGIN = RAW_BASE.replace(/\/api\/?$/, '').replace(/\/$/, '');
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

export const authApi = {
  getGoogleAuthUrl: async (): Promise<string> => {
    const { data } = await axiosInstance.get<{ state: string }>('/api/auth/oauth/state');
    const params = new URLSearchParams({
      client_id: GOOGLE_CLIENT_ID,
      redirect_uri: `${BACKEND_ORIGIN}/api/auth/google/callback`,
      response_type: 'code',
      scope: [
        'https://www.googleapis.com/auth/gmail.readonly',
        'https://www.googleapis.com/auth/gmail.modify',
        'https://www.googleapis.com/auth/calendar.events',
        'openid', 'email', 'profile',
      ].join(' '),
      access_type: 'offline',
      prompt: 'consent',
      state: data.state,
    });
    return `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;
  },


  getCurrentUser: async (): Promise<AuthResponse> => {
    const { data } = await axiosInstance.get<AuthResponse>('/api/auth/me');
    return data;
  },

  refreshSession: async (): Promise<AuthResponse> => {
    const { data } = await axiosInstance.post<AuthResponse>('/api/auth/refresh', {});
    return data;
  },

  exchangeCode: async (code: string): Promise<AuthResponse> => {
    const { data } = await axiosInstance.get<AuthResponse>('/api/auth/token', { params: { code } });
    return data;
  },

  updateProfile: async (params: {
    userRole?: UserRole;
    calendarSyncEnabled?: boolean;
    quietHoursStart?: number | null;
    quietHoursEnd?: number | null;
    mutedCategories?: string | null;
    digestEnabled?: boolean | null;
    digestHour?: number | null;
  }): Promise<AuthResponse> => {
    const { data } = await axiosInstance.put<AuthResponse>('/api/auth/profile', params);
    return data;
  },

  logout: async (token?: string | null): Promise<void> => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    await fetch(`${BACKEND_ORIGIN}/api/auth/logout`, {
      method: 'POST',
      credentials: 'include',
      headers,
    });
  },

  revokeAccess: async (token?: string | null): Promise<void> => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    // Raw fetch so a 401 here cannot trip the axios interceptor and bounce
    // the user back into a persisted session mid-logout.
    await fetch(`${BACKEND_ORIGIN}/api/auth/revoke`, {
      method: 'POST',
      credentials: 'include',
      headers,
    });
  },
};
