import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Flame, Inbox } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Tile, TileHead } from '../components/bento/Tile';
import { Placeholder } from '../components/bento/Placeholder';
import { priorityApi } from '../api/priorityApi';
import { queryKeys } from '../api/queryKeys';
import { CAT_COLORS } from '../utils/catColors';

const BANDS = [
  { key: 'HIGH',   label: 'Act now',    tone: 'var(--v-red)' },
  { key: 'MEDIUM', label: 'Today',      tone: 'var(--v-orange)' },
  { key: 'LOW',    label: 'When clear', tone: 'var(--v-green)' },
] as const;

export const PriorityInboxPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: emails = [], isLoading, isError, refetch } = useQuery({
    queryKey: [...queryKeys.emailPriority, 'page'],
    queryFn: () => priorityApi.getPriority(80),
    staleTime: 30_000,
  });

  const { data: suggestions = [] } = useQuery({
    queryKey: queryKeys.prioritySuggestions,
    queryFn: priorityApi.getSuggestions,
    staleTime: 30_000,
  });

  const queryClient = useQueryClient();

  const banded = useMemo(() => {
    return BANDS.map((band) => ({
      ...band,
      items: emails
        .filter((e) => e.priority === band.key)
        .sort(
          (a, b) =>
            new Date(b.receivedAt ?? 0).getTime() - new Date(a.receivedAt ?? 0).getTime(),
        ),
    }));
  }, [emails]);

  const total = banded.reduce((n, b) => n + b.items.length, 0);

  if (isError) {
    return (
      <AppShell title="Priority" subtitle="Ranked by what actually needs you">
        <Placeholder
          icon={<Flame size={26} />}
          tone="var(--v-critical)"
          headline="Couldn’t load priority mail"
          body="Check that the backend is running, then try again."
          points={['Act now', 'Today', 'When clear']}
          action={{ label: 'Retry', onClick: () => void refetch() }}
        />
      </AppShell>
    );
  }

  if (!isLoading && total === 0) {
    return (
      <AppShell title="Priority" subtitle="Ranked by what actually needs you">
        <Placeholder
          icon={<Flame size={26} />}
          tone="var(--v-critical)"
          headline="No prioritized mail yet"
          body="Sync Gmail from Home. Priority bands fill as Cortex classifies your real inbox."
          points={['Act now', 'Today', 'When clear']}
          action={{ label: 'Open home', onClick: () => navigate('/dashboard') }}
        />
      </AppShell>
    );
  }

  return (
    <AppShell
      title="Priority"
      subtitle={isLoading ? 'Loading priority…' : `${total} messages ranked by urgency`}
      actions={
        <button className="vbtn vbtn-quiet" onClick={() => navigate('/inbox')}>
          <Inbox size={16} /> All mail
        </button>
      }
    >
      <div className="bento">
        {banded.map((band, i) => (
          <Tile key={band.key} span={4} rule={band.tone} index={i}>
            <TileHead
              label={band.label}
              icon={<Flame size={17} />}
              tone={band.tone}
              right={<span className="v-readout v-readout-md">{band.items.length}</span>}
            />

            {band.items.length > 0 ? (
              <div className="stream">
                {band.items.slice(0, 10).map((e) => (
                  <div
                    key={e.id}
                    className="stream-row"
                    onClick={() => navigate(`/emails/${e.id}`)}
                  >
                    <span
                      className="dot"
                      style={{ ['--dot']: band.tone } as React.CSSProperties}
                    />
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <div
                        className="truncate"
                        style={{ fontSize: 13, fontWeight: e.isRead ? 600 : 700, color: 'var(--v-ink)' }}
                      >
                        {e.subject || '(no subject)'}
                      </div>
                      <div className="truncate v-meta" style={{ marginTop: 2 }}>
                        {e.senderName || e.senderEmail}
                        {' · '}
                        {CAT_COLORS[e.category]?.label ?? 'Other'}
                      </div>
                    </div>
                    <button
                      type="button"
                      className="vbtn vbtn-bare"
                      onClick={(ev) => {
                        ev.stopPropagation();
                        const action = e.isImportant ? priorityApi.unflag(e.id) : priorityApi.flag(e.id);
                        void action.then(() => {
                          queryClient.invalidateQueries({ queryKey: queryKeys.emailPriority });
                          queryClient.invalidateQueries({ queryKey: queryKeys.emails });
                        });
                      }}
                    >
                      {e.isImportant ? 'Unflag' : 'Flag'}
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="v-meta" style={{ margin: '8px 0 0' }}>Nothing in this band yet.</p>
            )}
          </Tile>
        ))}
        {suggestions.length > 0 && (
          <Tile span={12} index={4}>
            <TileHead label="Due this week" icon={<Flame size={17} />} tone="var(--v-ember)" />
            <div className="stream">
              {suggestions.map((e) => (
                <div key={e.id} className="stream-row" onClick={() => navigate(`/emails/${e.id}`)}>
                  <span className="dot" style={{ ['--dot']: 'var(--v-ember)' } as React.CSSProperties} />
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div className="truncate" style={{ fontSize: 13, fontWeight: 700 }}>{e.subject || '(no subject)'}</div>
                    <div className="v-meta truncate">{e.senderName || e.senderEmail}</div>
                  </div>
                </div>
              ))}
            </div>
          </Tile>
        )}
      </div>
    </AppShell>
  );
};
