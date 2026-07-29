import axiosInstance from './axiosInstance';
import type { AuthResponse, UserRole } from '../types/User';

// Resolve backend ORIGIN (no /api, no trailing slash)
const RAW_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const BACKEND_ORIGIN = RAW_BASE.replace(/\/api\/?$/, '').replace(/\/$/, '');

export const authApi = {
  /**
   * Hand off to the backend, which builds the Google consent URL and
   * redirects. The client id lives only on the server — assembling this URL
   * in the browser meant duplicating it into VITE_GOOGLE_CLIENT_ID, and when
   * that was unset the button silently sent users to Google with an empty
   * client_id and a bare "Error 400: invalid_request".
   */
  getGoogleAuthUrl: (): string => `${BACKEND_ORIGIN}/api/auth/google`,


  getCurrentUser: async (): Promise<AuthResponse> => {
    const { data } = await axiosInstance.get<AuthResponse>('/api/auth/me');
    return data;
  },

  exchangeCode: async (code: string): Promise<AuthResponse> => {
    const { data } = await axiosInstance.get<AuthResponse>('/api/auth/token', { params: { code } });
    return data;
  },

  updateProfile: async (params: { userRole?: UserRole; calendarSyncEnabled?: boolean }): Promise<AuthResponse> => {
    const { data } = await axiosInstance.put<AuthResponse>('/api/auth/profile', params);
    return data;
  },

  revokeAccess: async (): Promise<void> => {
    await axiosInstance.post('/api/auth/revoke');
  },
};
