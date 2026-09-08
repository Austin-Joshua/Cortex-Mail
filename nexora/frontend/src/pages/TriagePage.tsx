import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { CheckSquare, Clock, ListChecks } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { ConnectedSyncPipelineBanner } from '../components/common/SyncPipelineBanner';
import { Placeholder } from '../components/bento/Placeholder';
import { Tile, TileHead } from '../components/bento/Tile';
import { emailActionsApi } from '../api/emailActionsApi';
import { queryKeys } from '../api/queryKeys';

export const TriagePage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: actions = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['email-actions-pending'],
    queryFn: emailActionsApi.pending,
    staleTime: 15_000,
  });

  const complete = useMutation({
    mutationFn: emailActionsApi.complete,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['email-actions-pending'] });
      await queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary });
    },
  });

  const snooze = useMutation({
    mutationFn: (id: number) => emailActionsApi.snooze(id, 24),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['email-actions-pending'] });
      await queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary });
    },
  });

  if (isError) {
    return (
      <AppShell title="Triage" subtitle="Open follow-ups extracted from your mail">
        <ConnectedSyncPipelineBanner />
        <Placeholder
          icon={<ListChecks size={26} />}
          tone="var(--v-critical)"
          headline="Couldn’t load triage"
          body="Check that the backend is running, then try again."
          action={{ label: 'Retry', onClick: () => void refetch() }}
        />
      </AppShell>
    );
  }

  if (!isLoading && actions.length === 0) {
    return (
      <AppShell title="Triage" subtitle="Open follow-ups extracted from your mail">
        <ConnectedSyncPipelineBanner />
        <Placeholder
          icon={<ListChecks size={26} />}
          tone="var(--v-green)"
          headline="Queue clear"
          body="No open follow-ups right now. Sync and classify to discover new ones from mail."
          action={{ label: 'Open Home', onClick: () => navigate('/dashboard') }}
        />
      </AppShell>
    );
  }

  return (
    <AppShell
      title="Triage"
      subtitle={isLoading ? 'Loading follow-ups…' : `${actions.length} open follow-ups`}
    >
      <ConnectedSyncPipelineBanner />
      <Tile span={12} index={0}>
        <TileHead
          label="Needs a decision"
          icon={<ListChecks size={17} />}
          tone="var(--v-green)"
          right={<span className="v-readout v-readout-md">{actions.length}</span>}
        />
        <div className="stream">
          {actions.map((a) => (
            <div key={a.id} className="stream-row" style={{ alignItems: 'flex-start', gap: 12 }}>
              <span className="dot" style={{ ['--dot']: 'var(--v-green)', marginTop: 6 } as React.CSSProperties} />
              <div style={{ minWidth: 0, flex: 1 }}>
                <button
                  type="button"
                  className="vbtn vbtn-bare"
                  style={{ padding: 0, height: 'auto', fontWeight: 700, textAlign: 'left' }}
                  onClick={() => a.emailId && navigate(`/emails/${a.emailId}`)}
                >
                  {a.emailSubject || a.actionDescription || 'Follow-up'}
                </button>
                <div className="v-meta" style={{ marginTop: 4 }}>
                  {a.actionType || 'OTHER'}
                  {a.actionDescription ? ` · ${a.actionDescription}` : ''}
                  {a.deadline ? ` · due ${new Date(a.deadline).toLocaleString()}` : ''}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                <button
                  type="button"
                  className="vbtn vbtn-quiet"
                  style={{ height: 32 }}
                  disabled={snooze.isPending}
                  onClick={() => snooze.mutate(a.id)}
                  title="Snooze 24h"
                >
                  <Clock size={14} /> Snooze
                </button>
                <button
                  type="button"
                  className="vbtn vbtn-quiet"
                  style={{ height: 32 }}
                  disabled={complete.isPending}
                  onClick={() => complete.mutate(a.id)}
                >
                  <CheckSquare size={14} /> Done
                </button>
              </div>
            </div>
          ))}
        </div>
      </Tile>
    </AppShell>
  );
};
