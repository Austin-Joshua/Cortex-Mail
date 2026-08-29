import { useQuery } from '@tanstack/react-query';
import { emailApi } from '../api/emailApi';
import { useAuthStore } from '../store/authStore';

/** Warm mail list caches so Archive/Drafts/Inbox open instantly. */
export function useMailPrefetch() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useQuery({
    queryKey: ['emails', 'inbox', 'ALL', ''],
    queryFn: () => emailApi.getEmails({ page: 0, size: 500 }),
    staleTime: 90_000,
    enabled: isAuthenticated,
  });

  useQuery({
    queryKey: ['email-archived', ''],
    queryFn: () => emailApi.getArchived({ page: 0, size: 100 }),
    staleTime: 90_000,
    enabled: isAuthenticated,
  });

  useQuery({
    queryKey: ['email-drafts', ''],
    queryFn: () => emailApi.getDrafts({ page: 0, size: 100 }),
    staleTime: 90_000,
    enabled: isAuthenticated,
  });

  useQuery({
    queryKey: ['email-categories'],
    queryFn: emailApi.getCategoryCounts,
    staleTime: 90_000,
    enabled: isAuthenticated,
  });
}
