import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Clock, CalendarClock } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Placeholder } from '../components/bento/Placeholder';
import { Tile, TileHead } from '../components/bento/Tile';
import { dashboardApi } from '../api/dashboardApi';
import { queryKeys } from '../api/queryKeys';

type DeadlineRow = {
  id?: number;
  emailId?: number;
  subject?: string;
  title?: string;
  dueDate?: string;
  deadline?: string;
  deadlineDetected?: string;
};

function dueMs(d: DeadlineRow): number {
  const due = d.dueDate ?? d.deadline ?? d.deadlineDetected;
  return due ? new Date(due).getTime() : Number.POSITIVE_INFINITY;
}

export const ScheduledEmailsPage: React.FC = () => {
  const navigate = useNavigate();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: queryKeys.dashboardSummary,
    queryFn: dashboardApi.getSummary,
    staleTime: 60_000,
  });

  const { overdue, upcoming } = useMemo(() => {
    const raw = (data?.upcomingDeadlines ?? []) as DeadlineRow[];
    const now = Date.now();
    const overdueRows: DeadlineRow[] = [];
    const upcomingRows: DeadlineRow[] = [];
    for (const d of raw) {
      const t = dueMs(d);
      if (!Number.isFinite(t)) continue;
      if (t <= now) overdueRows.push(d);
      else upcomingRows.push(d);
    }
    overdueRows.sort((a, b) => dueMs(a) - dueMs(b));
    upcomingRows.sort((a, b) => dueMs(a) - dueMs(b));
    return { overdue: overdueRows, upcoming: upcomingRows };
  }, [data]);

  const deadlines = [...overdue, ...upcoming];

  if (isError) {
    return (
      <AppShell title="Deadlines" subtitle="Dates extracted from your synced mail">
        <Placeholder
          icon={<Clock size={26} />}
          tone="var(--v-critical)"
          headline="Couldn’t load deadlines"
          body="Check your connection, then try again."
          action={{ label: 'Retry', onClick: () => void refetch() }}
        />
      </AppShell>
    );
  }

  if (!isLoading && deadlines.length === 0) {
    return (
      <AppShell title="Deadlines" subtitle="Dates extracted from your synced mail">
        <Placeholder
          icon={<Clock size={26} />}
          tone="var(--v-ember)"
          headline="No deadlines detected yet"
          body="When Cortex finds due dates in your mail — including overdue ones — they show up here."
          points={['Extracted from real Gmail', 'Updates after sync + classify', 'Tap a row to open the mail']}
          action={{ label: 'Sync from Home', onClick: () => navigate('/dashboard') }}
        />
      </AppShell>
    );
  }

  const renderRow = (d: DeadlineRow, overdueRow: boolean) => {
    const due = d.dueDate ?? d.deadline ?? d.deadlineDetected;
    const emailId = d.emailId ?? d.id;
    return (
      <div
        key={`${emailId}-${due}-${overdueRow ? 'o' : 'u'}`}
        className="stream-row"
        onClick={() => emailId && navigate(`/emails/${emailId}`)}
        style={{ cursor: emailId ? 'pointer' : 'default' }}
      >
        <span
          className="dot"
          style={{ ['--dot']: overdueRow ? 'var(--v-critical)' : 'var(--v-ember)' } as React.CSSProperties}
        />
        <div style={{ minWidth: 0, flex: 1 }}>
          <div className="truncate" style={{ fontSize: 13, fontWeight: 700, color: 'var(--v-ink)' }}>
            {d.subject ?? d.title ?? 'Deadline'}
          </div>
          <div className="v-meta" style={{ marginTop: 2 }}>
            {overdueRow ? 'Overdue · ' : ''}
            {due ? new Date(due).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—'}
          </div>
        </div>
      </div>
    );
  };

  return (
    <AppShell
      title="Deadlines"
      subtitle={
        isLoading
          ? 'Loading deadlines…'
          : `${overdue.length} overdue · ${upcoming.length} upcoming`
      }
    >
      {overdue.length > 0 && (
        <Tile span={12} index={0}>
          <TileHead
            label="Overdue"
            icon={<CalendarClock size={17} />}
            tone="var(--v-critical)"
            right={<span className="v-readout v-readout-md">{overdue.length}</span>}
          />
          <div className="stream">{overdue.map((d) => renderRow(d, true))}</div>
        </Tile>
      )}
      <Tile span={12} index={1}>
        <TileHead
          label="Upcoming"
          icon={<CalendarClock size={17} />}
          tone="var(--v-ember)"
          right={<span className="v-readout v-readout-md">{upcoming.length}</span>}
        />
        <div className="stream">
          {isLoading && upcoming.length === 0 && overdue.length === 0 ? (
            <p className="v-meta" style={{ padding: '12px 0' }}>Loading…</p>
          ) : upcoming.length > 0 ? (
            upcoming.map((d) => renderRow(d, false))
          ) : (
            <p className="v-meta" style={{ padding: '12px 0' }}>No upcoming deadlines — overdue items are listed above.</p>
          )}
        </div>
      </Tile>
    </AppShell>
  );
};
