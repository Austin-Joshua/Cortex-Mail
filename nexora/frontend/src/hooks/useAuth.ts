import type { UserRole } from '../types/User';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { emailApi } from '../api/emailApi';
import { useNavigate } from 'react-router-dom';

export function useAuth() {
  const { user, token, isAuthenticated, setUser, setToken, logout } = useAuthStore();
  const navigate = useNavigate();

  const isGoogleConfigured = (() => {
    const id = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
    return Boolean(id) && !id.includes('your_google_client_id') && id.includes('.apps.googleusercontent.com');
  })();

  const handleGoogleLogin = () => {
    if (!isGoogleConfigured) {
      navigate('/?auth_error=oauth_not_configured', { replace: true });
      return;
    }
    window.location.href = authApi.getGoogleAuthUrl();
  };

  const handleLogout = async () => {
    try {
      await authApi.revokeAccess();
    } catch {
      // ignore
    }
    logout();
    navigate('/');
  };

  const updateProfile = async (params: { role?: UserRole; calendarSyncEnabled?: boolean }) => {
    const previousRole = user?.userRole;
    const authResponse = await authApi.updateProfile({
      userRole: params.role,
      calendarSyncEnabled: params.calendarSyncEnabled,
    });
    setToken(authResponse.token);
    setUser({
      userId: authResponse.userId,
      email: authResponse.email,
      name: authResponse.name,
      profilePictureUrl: authResponse.profilePictureUrl,
      userRole: authResponse.userRole,
      onboardingComplete: authResponse.onboardingComplete,
      calendarSyncEnabled: authResponse.calendarSyncEnabled,
      lastSyncedAt: authResponse.lastSyncedAt,
    });
    if (params.role && params.role !== previousRole) {
      void emailApi.classifyInbox({ force: true }).catch(() => {});
    }
  };

  const updateRole = async (role: UserRole) => {
    await updateProfile({ role });
  };

  return { user, token, isAuthenticated, handleGoogleLogin, isGoogleConfigured, handleLogout, updateRole, updateProfile };
}
