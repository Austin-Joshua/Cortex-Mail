import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const RAW_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const API_BASE_URL = RAW_BASE.replace(/\/api\/?$/, '').replace(/\/$/, '');

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let handlingUnauthorized = false;
let refreshPromise: Promise<boolean> | null = null;

async function tryRefreshSession(): Promise<boolean> {
  try {
    const { data } = await axios.post(
      `${API_BASE_URL}/api/auth/refresh`,
      {},
      { withCredentials: true, headers: { 'Content-Type': 'application/json' } },
    );
    if (data?.token) {
      useAuthStore.getState().setToken(data.token);
      if (data.userId) {
        useAuthStore.getState().setUser({
          userId: data.userId,
          email: data.email,
          name: data.name,
          profilePictureUrl: data.profilePictureUrl,
          userRole: data.userRole,
          onboardingComplete: data.onboardingComplete,
          calendarSyncEnabled: data.calendarSyncEnabled,
          lastSyncedAt: data.lastSyncedAt,
          quietHoursStart: data.quietHoursStart,
          quietHoursEnd: data.quietHoursEnd,
          mutedCategories: data.mutedCategories,
          digestEnabled: data.digestEnabled,
          digestHour: data.digestHour,
        });
      }
      return true;
    }
    return Boolean(data);
  } catch {
    return false;
  }
}

axiosInstance.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const original = error.config;
    const url = String(original?.url ?? '');
    const isPublicAuth =
      url.includes('/api/auth/token')
      || url.includes('/api/auth/oauth/state')
      || url.includes('/api/auth/google')
      || url.includes('/api/auth/refresh');

    if (status === 401 && !isPublicAuth && original && !(original as { _retry?: boolean })._retry) {
      (original as { _retry?: boolean })._retry = true;
      if (!refreshPromise) {
        refreshPromise = tryRefreshSession().finally(() => {
          refreshPromise = null;
        });
      }
      const ok = await refreshPromise;
      if (ok) {
        const token = useAuthStore.getState().token;
        if (token) {
          original.headers = original.headers ?? {};
          original.headers.Authorization = `Bearer ${token}`;
        }
        return axiosInstance(original);
      }
      if (!handlingUnauthorized) {
        handlingUnauthorized = true;
        useAuthStore.getState().logout();
        if (window.location.pathname !== '/') {
          window.location.replace('/');
        }
        window.setTimeout(() => {
          handlingUnauthorized = false;
        }, 1500);
      }
    }
    return Promise.reject(error);
  },
);

export default axiosInstance;
