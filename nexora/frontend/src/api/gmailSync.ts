import type { QueryClient } from '@tanstack/react-query';
import { emailApi } from './emailApi';
import type { GmailSyncResult } from '../types/Email';
import { SYNC_INVALIDATION_KEYS, queryKeys } from './queryKeys';

/** Single post-sync cache refresh used by TopBar and the inbox pipeline. */
export async function invalidateAfterGmailSync(queryClient: QueryClient) {
  await Promise.all(
    SYNC_INVALIDATION_KEYS.map((queryKey) =>
      queryClient.invalidateQueries({ queryKey: [...queryKey] }),
    ),
  );
}

export async function runGmailSync(queryClient: QueryClient): Promise<GmailSyncResult> {
  const result = await emailApi.syncEmails();

  if (result.labelCounts) {
    queryClient.setQueryData(queryKeys.gmailLabelCounts, result.labelCounts);
  }

  if (result.syncMode === 'SKIPPED') {
    await queryClient.invalidateQueries({ queryKey: queryKeys.emails });
    await queryClient.invalidateQueries({ queryKey: queryKeys.syncStatus });
    return result;
  }

  await invalidateAfterGmailSync(queryClient);
  return result;
}
