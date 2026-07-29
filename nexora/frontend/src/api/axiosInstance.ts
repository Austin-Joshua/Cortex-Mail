import axios from 'axios';
import { useAuthStore } from '../store/authStore';

// Base URL is the backend ORIGIN (no /api suffix) — all paths already include /api/...
// e.g. VITE_API_BASE_URL = https://nexora-75kw.onrender.com  (no trailing slash, no /api)
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


// Request interceptor — attach JWT
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('nexora_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clearing the token alone was not enough to end the session. The gate
      // on every protected route reads `isAuthenticated` out of the persisted
      // zustand store ('nexora_auth'), which this left set to true — so an
      // expired token still rendered the page, its first request 401'd, and
      // the redirect below bounced straight back in. ('nexora_user' was never
      // a key that existed.) Going through logout() clears store and storage
      // together, so the session actually ends.
      useAuthStore.getState().logout();

      // Already on the landing page: redirecting again would reload in a loop.
      if (window.location.pathname !== '/') {
        window.location.href = '/';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
