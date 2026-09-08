import type { AuthResponse, User } from '../types/User';
import { useAuthStore } from '../store/authStore';

/** Map /me, /refresh, /token, /profile payloads into the auth store. */
export function applyAuthResponse(authResponse: AuthResponse) {
  const { setUser, setToken } = useAuthStore.getState();
  if (authResponse.token) {
    setToken(authResponse.token);
  }
  const user: User = {
    userId: authResponse.userId,
    email: authResponse.email,
    name: authResponse.name,
    profilePictureUrl: authResponse.profilePictureUrl,
    userRole: authResponse.userRole,
    onboardingComplete: authResponse.onboardingComplete,
    calendarSyncEnabled: authResponse.calendarSyncEnabled,
    lastSyncedAt: authResponse.lastSyncedAt,
    quietHoursStart: authResponse.quietHoursStart,
    quietHoursEnd: authResponse.quietHoursEnd,
    mutedCategories: authResponse.mutedCategories,
    digestEnabled: authResponse.digestEnabled,
    digestHour: authResponse.digestHour,
  };
  setUser(user);
}
