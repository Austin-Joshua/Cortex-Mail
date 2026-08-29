import { keepPreviousData, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { emailApi } from '../api/emailApi';
import { useEmailStore } from '../store/emailStore';

export interface UseEmailsOptions {
  page?: number;
  size?: number;
  /** When false, TopBar search does not hit the API (inbox uses tabs only). Default true. */
  useGlobalSearch?: boolean;
  /** Skip category/label count queries (lighter for list-only pages). */
  skipCounts?: boolean;
}

export function useEmails(page = 0, size = 80, options: UseEmailsOptions = {}) {
  const { useGlobalSearch = true, skipCounts = false } = options;
  const { activeCategory, activePriority, searchQuery } = useEmailStore();
  const queryClient = useQueryClient();

  const params = {
    category: activeCategory !== 'ALL' ? activeCategory : undefined,
    priority: activePriority !== 'ALL' ? activePriority : undefined,
    search: useGlobalSearch && searchQuery ? searchQuery : undefined,
    page,
    size,
  };

  const emailsQuery = useQuery({
    queryKey: ['emails', params],
    queryFn: () => emailApi.getEmails(params),
    staleTime: 60_000,
    placeholderData: keepPreviousData,
  });

  const categoryCountsQuery = useQuery({
    queryKey: ['email-categories'],
    queryFn: emailApi.getCategoryCounts,
    staleTime: 120_000,
    enabled: !skipCounts,
  });

  const labelCountsQuery = useQuery({
    queryKey: ['gmail-label-counts'],
    queryFn: emailApi.getGmailLabelCounts,
    staleTime: 120_000,
    enabled: !skipCounts,
  });

  const markReadMutation = useMutation({
    mutationFn: emailApi.markRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emails'] });
      queryClient.invalidateQueries({ queryKey: ['gmail-label-counts'] });
    },
  });

  const isInitialLoad = emailsQuery.isLoading && !emailsQuery.data;

  return {
    emails: emailsQuery.data?.content ?? [],
    totalPages: emailsQuery.data?.totalPages ?? 0,
    totalElements: emailsQuery.data?.totalElements ?? 0,
    isLoading: isInitialLoad,
    isFetching: emailsQuery.isFetching,
    isError: emailsQuery.isError,
    categoryCounts: categoryCountsQuery.data ?? {},
    labelCounts: labelCountsQuery.data ?? {},
    inboxUnread: labelCountsQuery.data?.INBOX?.messagesUnread ?? 0,
    markRead: markReadMutation.mutate,
  };
}
