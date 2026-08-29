import { useQuery } from '@tanstack/react-query';
import { emailApi } from '../api/emailApi';

/** Badge counts only — avoids loading the full inbox on every page. */
export function useInboxUnread() {
  const { data } = useQuery({
    queryKey: ['gmail-label-counts'],
    queryFn: emailApi.getGmailLabelCounts,
    staleTime: 120_000,
  });

  return data?.INBOX?.messagesUnread ?? 0;
}
