import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Flame, Inbox } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Tile, TileHead } from '../components/bento/Tile';
import { Placeholder } from '../components/bento/Placeholder';
import { useEmails } from '../hooks/useEmails';
import { CAT_COLORS } from '../utils/catColors';

const BANDS = [
  { key: 'HIGH',   label: 'Act now',    tone: 'var(--v-ember)' },
  { key: 'MEDIUM', label: 'Today',      tone: 'var(--v-signal)' },
  { key: 'LOW',    label: 'When clear', tone: 'var(--v-ink-3)' },
] as const;

export const PriorityInboxPage: React.FC = () => {
  const navigate = useNavigate();
  const { emails, isLoading } = useEmails();

  const banded = useMemo(() => {
    const unread = emails.filter((e) => !e.isRead);
    return BANDS.map((band) => ({
      ...band,
      items: unread
        .filter((e) => e.priority === band.key)
        .sort(
          (a, b) =>
            new Date(b.receivedAt ?? 0).getTime() - new Date(a.receivedAt ?? 0).getTime(),
        ),
    }));
  }, [emails]);

  const total = banded.reduce((n, b) => n + b.items.length, 0);

  if (!isLoading && total === 0) {
    return (
      <AppShell title="Priority" subtitle="Ranked by what actually needs you">
        <Placeholder
          icon={<Flame size={26} />}
          tone="var(--v-critical)"
          headline="Nothing is waiting on you"
          body="Everything in your inbox is read. When new mail lands, Velocity ranks it here by urgency so the top of the list is always the thing to do next."
          points={['Act now', 'Today', 'When clear']}
          action={{ label: 'Open inbox', onClick: () => navigate('/inbox') }}
        />
      </AppShell>
    );
  }

  return (
    <AppShell
      title="Priority"
      subtitle={isLoading ? 'Ranking your inbox…' : `${total} unread, ranked by urgency`}
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
                {band.items.slice(0, 8).map((e) => (
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
                        style={{ fontSize: 13, fontWeight: 700, color: 'var(--v-ink)' }}
                      >
                        {e.subject || '(no subject)'}
                      </div>
                      <div className="truncate v-meta" style={{ marginTop: 2 }}>
                        {e.senderName || e.senderEmail}
                        {' · '}
                        {CAT_COLORS[e.category]?.label ?? 'Other'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="v-meta" style={{ paddingBlock: 12 }}>
                {isLoading ? 'Loading…' : 'Clear.'}
              </p>
            )}
          </Tile>
        ))}
      </div>
    </AppShell>
  );
};
