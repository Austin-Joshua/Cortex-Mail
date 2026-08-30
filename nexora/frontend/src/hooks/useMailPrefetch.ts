import { useQueryClient } from '@tanstack/react-query';
import { emailApi } from '../api/emailApi';
import { queryKeys } from '../api/queryKeys';

/** Prefetch drafts/archive only when the user intends to open those routes. */
export function useMailPrefetch() {
  const queryClient = useQueryClient();

  const prefetchDrafts = () => {
    void queryClient.prefetchQuery({
      queryKey: queryKeys.emailDrafts(''),
      queryFn: () => emailApi.getDrafts({ page: 0, size: 100 }),
      staleTime: 90_000,
    });
  };

  const prefetchArchive = () => {
    void queryClient.prefetchQuery({
      queryKey: queryKeys.emailArchived(''),
      queryFn: () => emailApi.getArchived({ page: 0, size: 100 }),
      staleTime: 90_000,
    });
  };

  return { prefetchDrafts, prefetchArchive };
}
