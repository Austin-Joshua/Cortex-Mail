import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { applyAuthResponse } from '../utils/applyAuthResponse';
import { useAuthStore } from '../store/authStore';

/**
 * Handles the OAuth callback redirect from the backend.
 * Backend redirects to /auth/callback?code=UUID
 * Google may also redirect here with ?error=access_denied&error_description=...
 */
export const AuthCallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const setSessionReady = useAuthStore((s) => s.setSessionReady);
  const hasCalled = React.useRef(false);

  useEffect(() => {
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
          applyAuthResponse(authResponse);
          setSessionReady(true);
          navigate(authResponse.onboardingComplete ? '/dashboard' : '/onboarding', { replace: true });
        })
        .catch((err) => {
          clearTimeout(timeout);
          console.error('Failed to exchange code:', err);
          navigate('/?auth_error=exchange_failed', { replace: true });
        });

      return () => clearTimeout(timeout);
    } else if (!hasCalled.current) {
      navigate('/', { replace: true });
    }
  }, [searchParams, navigate, setSessionReady]);

  return <LoadingSpinner fullScreen label="Signing you in..." />;
};
