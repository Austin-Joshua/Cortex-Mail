import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

/**
 * Handles the OAuth callback redirect from the backend.
 * Backend redirects to /auth/callback?code=UUID
 * Google may also redirect here with ?error=access_denied&error_description=...
 */
export const AuthCallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setToken, setUser } = useAuthStore();
  const hasCalled = React.useRef(false);

  useEffect(() => {
    // Handle Google OAuth errors (e.g., "access_denied", "no_access")
    const error = searchParams.get('error');
    if (error) {
      console.warn('Google OAuth error:', error, searchParams.get('error_description'));
      navigate(`/?auth_error=${encodeURIComponent(error)}`, { replace: true });
      return;
    }

    const code = searchParams.get('code');
    if (code && !hasCalled.current) {
      hasCalled.current = true;
      
      const timeout = setTimeout(() => {
        console.error('Authentication request timed out');
        navigate('/?auth_error=timeout', { replace: true });
      }, 15000);

      authApi.exchangeCode(code)
        .then((authResponse) => {
          clearTimeout(timeout);
          setToken(authResponse.token);
          setUser({
            userId: authResponse.userId,
            email: authResponse.email,
            name: authResponse.name,
            profilePictureUrl: authResponse.profilePictureUrl ?? undefined,
            userRole: authResponse.userRole,
            onboardingComplete: authResponse.onboardingComplete,
            calendarSyncEnabled: authResponse.calendarSyncEnabled ?? true,
            lastSyncedAt: authResponse.lastSyncedAt,
          });
          navigate(authResponse.onboardingComplete ? '/dashboard' : '/onboarding', { replace: true });
        })
        .catch((err) => {
          clearTimeout(timeout);
          console.error('Failed to exchange code:', err);
          navigate('/?auth_error=exchange_failed', { replace: true });
        });

      return () => clearTimeout(timeout);
    } else if (!hasCalled.current) {
      // No code and no error — something unexpected, go home
      navigate('/', { replace: true });
    }
  }, [searchParams, navigate, setToken, setUser]);

  return <LoadingSpinner fullScreen label="Signing you in..." />;
};
