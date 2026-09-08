import axios from 'axios';
import { useAuthStore } from '../store/authStore';

// Base URL is the backend ORIGIN (no /api suffix) — all paths already include /api/...
// e.g. VITE_API_BASE_URL = https://api.example.com (no trailing slash, no /api)
const RAW_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
// Normalise: strip any trailing /api or / so we get a clean origin
const API_BASE_URL = RAW_BASE.replace(/\/api\/?$/, '').replace(/\/$/, '');

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let handlingUnauthorized = false;

// Request interceptor — attach JWT
axiosInstance.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401 once (avoid thrash from parallel failures)
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = String(error.config?.url ?? '');
    const isPublicAuth =
      url.includes('/api/auth/token')
      || url.includes('/api/auth/oauth/state')
      || url.includes('/api/auth/google');

    if (status === 401 && !isPublicAuth && !handlingUnauthorized) {
      handlingUnauthorized = true;
      useAuthStore.getState().logout();
      if (window.location.pathname !== '/') {
        window.location.replace('/');
      }
      window.setTimeout(() => {
        handlingUnauthorized = false;
      }, 1500);
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
