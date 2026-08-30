import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Clock, CalendarClock } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Placeholder } from '../components/bento/Placeholder';
import { Tile, TileHead } from '../components/bento/Tile';
import { dashboardApi } from '../api/dashboardApi';
import { queryKeys } from '../api/queryKeys';

export const ScheduledEmailsPage: React.FC = () => {
  const navigate = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.dashboardSummary,
    queryFn: dashboardApi.getSummary,
    staleTime: 60_000,
  });

  const deadlines = useMemo(() => {
    const raw = data?.upcomingDeadlines ?? [];
    return [...raw]
      .filter((d: { dueDate?: string; deadline?: string; deadlineDetected?: string }) => {
        const due = d.dueDate ?? d.deadline ?? d.deadlineDetected;
        return due && new Date(due).getTime() > Date.now();
      })
      .sort((a: { dueDate?: string; deadline?: string; deadlineDetected?: string }, b: typeof a) => {
        const da = new Date(a.dueDate ?? a.deadline ?? a.deadlineDetected ?? 0).getTime();
        const db = new Date(b.dueDate ?? b.deadline ?? b.deadlineDetected ?? 0).getTime();
        return da - db;
      });
  }, [data]);

  if (!isLoading && deadlines.length === 0) {
    return (
      <AppShell title="Deadlines" subtitle="Dates extracted from your synced mail">
        <Placeholder
          icon={<Clock size={26} />}
          tone="var(--v-ember)"
          headline="No upcoming deadlines"
          body="When Cortex finds due dates in your mail, they show up here. Send-later mail is not available yet."
          points={['Extracted from real Gmail', 'Updates after sync + classify', 'Tap a row to open the mail']}
          action={{ label: 'Sync inbox', onClick: () => navigate('/dashboard') }}
        />
      </AppShell>
    );
  }

  return (
    <AppShell
      title="Deadlines"
      subtitle={isLoading ? 'Loading deadlines…' : `${deadlines.length} upcoming from your mail`}
    >
      <Tile span={12} index={0}>
        <TileHead
          label="Upcoming deadlines"
          icon={<CalendarClock size={17} />}
          tone="var(--v-ember)"
          right={<span className="v-readout v-readout-md">{deadlines.length}</span>}
        />
        <div className="stream">
          {deadlines.map((d: {
            id?: number;
            emailId?: number;
            subject?: string;
            title?: string;
            dueDate?: string;
            deadline?: string;
            deadlineDetected?: string;
          }) => {
            const due = d.dueDate ?? d.deadline ?? d.deadlineDetected;
            const emailId = d.emailId ?? d.id;
            return (
              <div
                key={`${emailId}-${due}`}
                className="stream-row"
                onClick={() => emailId && navigate(`/emails/${emailId}`)}
                style={{ cursor: emailId ? 'pointer' : 'default' }}
              >
                <span className="dot" style={{ ['--dot']: 'var(--v-ember)' } as React.CSSProperties} />
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div className="truncate" style={{ fontSize: 13, fontWeight: 700, color: 'var(--v-ink)' }}>
                    {d.subject ?? d.title ?? 'Deadline'}
                  </div>
                  <div className="v-meta" style={{ marginTop: 2 }}>
                    {due ? new Date(due).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—'}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </Tile>
    </AppShell>
  );
};
