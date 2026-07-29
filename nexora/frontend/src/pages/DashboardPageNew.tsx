import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Inbox, Timer, ListChecks, Activity, ArrowUpRight, Sparkles,
  CalendarClock, Flame, Gauge as GaugeIcon,
} from 'lucide-react';
import { dashboardApi } from '../api/dashboardApi';
import { AppShell } from '../components/layout/AppShell';
import { useAuthStore } from '../store/authStore';
import { useEmails } from '../hooks/useEmails';
import { Tile, TileHead } from '../components/bento/Tile';
import { Gauge } from '../components/bento/Gauge';
import { CAT_COLORS } from '../utils/catColors';

const DAY_LABELS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

/** Flow zones are fixed bands across the working day. */
const FLOW_ZONES = [
  { name: 'Deep Focus',    from: 9,  to: 12, tone: 'var(--v-signal)',   quiet: true },
  { name: 'Collaboration', from: 12, to: 15, tone: 'var(--v-pulse)',    quiet: false },
  { name: 'Rapid Fire',    from: 15, to: 17, tone: 'var(--v-amber)',    quiet: false },
  { name: 'Reflection',    from: 17, to: 19, tone: 'var(--v-ink-3)',    quiet: true },
];

export const DashboardPageNew: React.FC = () => {
  const { user } = useAuthStore();
  const { emails, categoryCounts, isLoading } = useEmails();
  const navigate = useNavigate();

  const { data } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: dashboardApi.getSummary,
    staleTime: 300_000,
  });

  const now = new Date();
  const hour = now.getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const firstName = user?.name?.split(' ')[0] ?? 'there';

  const unread = data?.unreadCount ?? 0;
  const deadlines = data?.upcomingDeadlines ?? [];
  const actions = data?.pendingActions ?? [];

  /** Overdue deadlines drag the score hardest — they are the real friction. */
  const overdue = useMemo(
    () =>
      deadlines.filter((d: any) => {
        const due = d?.dueDate ?? d?.deadline ?? d?.deadlineDetected;
        return due && new Date(due).getTime() < Date.now();
      }).length,
    [deadlines],
  );

  /**
   * Velocity Score — derived from real backlog, not a decorative number.
   * Starts at 100 and is debited by the three things that slow you down.
   */
  const score = useMemo(() => {
    const raw =
      100 -
      Math.min(45, unread * 1.2) -
      Math.min(25, actions.length * 3) -
      Math.min(20, overdue * 5);
    return Math.max(0, Math.round(raw));
  }, [unread, actions.length, overdue]);

  const scoreTone =
    score >= 75 ? 'var(--v-pulse)' : score >= 45 ? 'var(--v-signal)' : 'var(--v-amber)';
  const scoreVerdict =
    score >= 75 ? 'Running clear' : score >= 45 ? 'Some drag' : 'Backlog building';

  /** Last 7 days of received mail, oldest → newest. */
  const week = useMemo(() => {
    const buckets = Array.from({ length: 7 }, (_, i) => {
      const d = new Date();
      d.setHours(0, 0, 0, 0);
      d.setDate(d.getDate() - (6 - i));
      return { day: d.getDay(), date: d, count: 0 };
    });
    const start = buckets[0].date.getTime();
    for (const e of emails) {
      if (!e.receivedAt) continue;
      const t = new Date(e.receivedAt).getTime();
      if (Number.isNaN(t) || t < start) continue;
      const idx = Math.floor((t - start) / 86_400_000);
      if (idx >= 0 && idx < 7) buckets[idx].count += 1;
    }
    return buckets;
  }, [emails]);

  const weekPeak = Math.max(1, ...week.map((d) => d.count));
  const weekTotal = week.reduce((s, d) => s + d.count, 0);

  /** Highest-signal unread mail, newest first. */
  const priorityStream = useMemo(
    () =>
      [...emails]
        .filter((e) => !e.isRead)
        .sort((a, b) => {
          const rank = { HIGH: 0, MEDIUM: 1, LOW: 2 } as const;
          const byPriority = rank[a.priority] - rank[b.priority];
          if (byPriority !== 0) return byPriority;
          return new Date(b.receivedAt ?? 0).getTime() - new Date(a.receivedAt ?? 0).getTime();
        })
        .slice(0, 6),
    [emails],
  );

  const topCategories = useMemo(
    () =>
      Object.entries(categoryCounts)
        .filter(([, n]) => (n as number) > 0)
        .sort((a, b) => (b[1] as number) - (a[1] as number))
        .slice(0, 5) as [string, number][],
    [categoryCounts],
  );
  const catMax = Math.max(1, ...topCategories.map(([, n]) => n));

  const activeZone = FLOW_ZONES.find((z) => hour >= z.from && hour < z.to);

  return (
    <AppShell title={`${greeting}, ${firstName}`} subtitle={`${scoreVerdict} · ${weekTotal} messages this week`}>
      <div className="bento">

        {/* ---- HERO: the instrument ---- */}
        <Tile span={5} rows={2} feature index={0}>
          <TileHead
            label="Velocity Score"
            icon={<GaugeIcon size={17} />}
            tone={scoreTone}
            right={
              <span className={`delta ${score >= 75 ? 'delta-up' : score >= 45 ? 'delta-flat' : 'delta-down'}`}>
                {scoreVerdict}
              </span>
            }
          />

          <div
            className="tile-body"
            style={{ alignItems: 'center', justifyContent: 'center', gap: 18, paddingBlock: 8 }}
          >
            <Gauge value={score} tone={scoreTone} label="of 100" />

            <div style={{ display: 'flex', gap: 8, width: '100%' }}>
              {[
                { k: 'Backlog', v: unread, tone: 'var(--v-signal)' },
                { k: 'Actions', v: actions.length, tone: 'var(--v-amber)' },
                { k: 'Overdue', v: overdue, tone: overdue ? 'var(--v-critical)' : 'var(--v-ink-4)' },
              ].map((s) => (
                <div
                  key={s.k}
                  style={{
                    flex: 1,
                    minWidth: 0,
                    padding: '10px 12px',
                    borderRadius: 12,
                    background: 'var(--v-panel-2)',
                    border: '1px solid var(--v-hairline)',
                  }}
                >
                  <div className="v-readout" style={{ fontSize: 20, color: s.tone }}>
                    {s.v}
                  </div>
                  <div className="v-label truncate" style={{ marginTop: 5 }}>{s.k}</div>
                </div>
              ))}
            </div>
          </div>
        </Tile>

        {/* ---- Metric chips ---- */}
        <Tile span={3} rule="var(--v-signal)" index={1} onClick={() => navigate('/inbox')}>
          <TileHead label="Unread" icon={<Inbox size={17} />} tone="var(--v-signal)"
            right={<ArrowUpRight size={15} style={{ color: 'var(--v-ink-4)' }} />} />
          <div className="v-readout v-readout-lg">{unread}</div>
          <p className="v-meta">{emails.length ? `of ${emails.length} synced` : 'nothing synced yet'}</p>
        </Tile>

        <Tile span={4} rule="var(--v-amber)" index={2} onClick={() => navigate('/priority')}>
          <TileHead label="Deadlines" icon={<Timer size={17} />} tone="var(--v-amber)"
            right={overdue > 0 ? <span className="delta delta-down">{overdue} overdue</span> : undefined} />
          <div className="v-readout v-readout-lg">{deadlines.length}</div>
          <p className="v-meta">
            {deadlines.length
              ? `next: ${nextDeadlineLabel(deadlines)}`
              : 'no dated commitments detected'}
          </p>
        </Tile>

        <Tile span={3} rule="var(--v-pulse)" index={3}>
          <TileHead label="Actions" icon={<ListChecks size={17} />} tone="var(--v-pulse)" />
          <div className="v-readout v-readout-lg">{actions.length}</div>
          <p className="v-meta">extracted from your mail</p>
        </Tile>

        <Tile span={4} index={4} onClick={() => navigate('/analytics')}>
          <TileHead label="Volume · 7d" icon={<Activity size={17} />} tone="var(--v-signal)"
            right={<span className="v-num v-label">{weekTotal}</span>} />
          <div className="spark" style={{ minHeight: 44 }}>
            {week.map((d, i) => (
              <i
                key={i}
                data-peak={d.count === weekPeak && d.count > 0 ? '1' : '0'}
                style={{ height: `${Math.max(6, (d.count / weekPeak) * 100)}%` }}
                title={`${d.count} on ${d.date.toLocaleDateString()}`}
              />
            ))}
          </div>
          <div style={{ display: 'flex', gap: 5 }}>
            {week.map((d, i) => (
              <span key={i} className="v-label" style={{ flex: 1, textAlign: 'center' }}>
                {DAY_LABELS[d.day]}
              </span>
            ))}
          </div>
        </Tile>

        {/* ---- Priority stream ---- */}
        <Tile span={7} rows={2} index={5}>
          <TileHead
            label="Needs you first"
            icon={<Flame size={17} />}
            tone="var(--v-critical)"
            right={
              <button className="vbtn vbtn-bare" onClick={() => navigate('/inbox')}>
                Open inbox <ArrowUpRight size={14} />
              </button>
            }
          />

          {priorityStream.length > 0 ? (
            <div className="stream">
              {priorityStream.map((e) => (
                <div key={e.id} className="stream-row" onClick={() => navigate(`/emails/${e.id}`)}>
                  <span
                    className="dot"
                    style={{
                      ['--dot' as string]:
                        e.priority === 'HIGH'
                          ? 'var(--v-critical)'
                          : e.priority === 'MEDIUM'
                          ? 'var(--v-amber)'
                          : 'var(--v-ink-4)',
                    } as React.CSSProperties}
                  />
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div className="truncate" style={{ fontSize: 13, fontWeight: 700, color: 'var(--v-ink)' }}>
                      {e.subject || '(no subject)'}
                    </div>
                    <div className="truncate v-meta" style={{ marginTop: 2 }}>
                      {e.senderName || e.senderEmail}
                    </div>
                  </div>
                  <span
                    className="chip"
                    style={{
                      color: CAT_COLORS[e.category]?.color ?? 'var(--v-ink-3)',
                      borderColor: 'var(--v-hairline)',
                      flexShrink: 0,
                    }}
                  >
                    {CAT_COLORS[e.category]?.label ?? 'Other'}
                  </span>
                  <span className="v-meta v-num" style={{ flexShrink: 0, width: 52, textAlign: 'right' }}>
                    {shortTime(e.receivedAt)}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              loading={isLoading}
              text={isLoading ? 'Reading your inbox…' : 'Inbox clear. Nothing waiting on you.'}
            />
          )}
        </Tile>

        {/* ---- Category mix ---- */}
        <Tile span={5} rows={2} index={6}>
          <TileHead label="Where mail comes from" icon={<Sparkles size={17} />} tone="var(--v-signal)" />
          {topCategories.length > 0 ? (
            <div
              className="tile-body"
              style={{ justifyContent: 'space-between', gap: 14, paddingBlock: 4 }}
            >
              {topCategories.map(([cat, n]) => {
                const cfg = CAT_COLORS[cat] ?? { label: cat, color: 'var(--v-ink-3)' };
                return (
                  <button
                    key={cat}
                    onClick={() => navigate(`/inbox?category=${cat}`)}
                    style={{
                      display: 'block', width: '100%', textAlign: 'left',
                      background: 'none', border: 'none', padding: 0, cursor: 'pointer',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                      <span style={{ fontSize: 12.5, fontWeight: 600, color: 'var(--v-ink-2)' }}>
                        {cfg.label}
                      </span>
                      <span className="v-num" style={{ fontSize: 12.5, fontWeight: 800, color: 'var(--v-ink)' }}>
                        {n}
                      </span>
                    </div>
                    <div className="meter" style={{ ['--meter' as string]: cfg.color } as React.CSSProperties}>
                      <span style={{ width: `${(n / catMax) * 100}%` }} />
                    </div>
                  </button>
                );
              })}
            </div>
          ) : (
            <EmptyState loading={isLoading} text="Sync your inbox to see the mix." />
          )}
        </Tile>

        {/* ---- Flow zone strip ---- */}
        <Tile span={12} index={7}>
          <TileHead
            label="Flow zones"
            icon={<CalendarClock size={17} />}
            tone={activeZone?.tone ?? 'var(--v-ink-3)'}
            right={
              <span className="v-meta">
                {activeZone ? (
                  <><span className="dot v-breathe" style={{ ['--dot' as string]: activeZone.tone, display: 'inline-block', marginRight: 6 } as React.CSSProperties} />
                  In {activeZone.name}</>
                ) : 'Outside working hours'}
              </span>
            }
          />
          <div className="v-xscroll">
            <div style={{ display: 'flex', gap: 10, minWidth: 560 }}>
              {FLOW_ZONES.map((z) => {
                const on = z === activeZone;
                return (
                  <div
                    key={z.name}
                    style={{
                      flex: 1,
                      minWidth: 128,
                      padding: '12px 14px',
                      borderRadius: 14,
                      background: on ? 'var(--v-panel-2)' : 'transparent',
                      border: `1px solid ${on ? 'var(--v-hairline-2)' : 'var(--v-hairline)'}`,
                      opacity: on ? 1 : 0.72,
                      transition: 'all var(--v-fast)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 8 }}>
                      <span className="dot" style={{ ['--dot' as string]: z.tone } as React.CSSProperties} />
                      <span style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--v-ink)' }}>{z.name}</span>
                    </div>
                    <div className="v-num v-meta">
                      {fmtHour(z.from)} — {fmtHour(z.to)}
                    </div>
                    <div className="v-label" style={{ marginTop: 7 }}>
                      {z.quiet ? 'Notifications muted' : 'Notifications live'}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </Tile>
      </div>
    </AppShell>
  );
};

/* ------------------------------------------------------------------ */

const EmptyState: React.FC<{ text: string; loading?: boolean }> = ({ text, loading }) => (
  <div
    style={{
      flex: 1,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 120,
      color: 'var(--v-ink-3)',
      fontSize: 13,
      textAlign: 'center',
      padding: 16,
    }}
  >
    {loading ? <span className="v-breathe">{text}</span> : text}
  </div>
);

function fmtHour(h: number) {
  const suffix = h >= 12 ? 'pm' : 'am';
  const display = h % 12 === 0 ? 12 : h % 12;
  return `${display}${suffix}`;
}

function shortTime(iso?: string) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const mins = Math.floor((Date.now() - d.getTime()) / 60_000);
  if (mins < 60) return `${Math.max(1, mins)}m`;
  if (mins < 1440) return `${Math.floor(mins / 60)}h`;
  return `${Math.floor(mins / 1440)}d`;
}

function nextDeadlineLabel(deadlines: any[]) {
  const dated = deadlines
    .map((d) => d?.dueDate ?? d?.deadline ?? d?.deadlineDetected)
    .filter(Boolean)
    .map((s: string) => new Date(s))
    .filter((d: Date) => !Number.isNaN(d.getTime()))
    .sort((a: Date, b: Date) => a.getTime() - b.getTime());
  if (!dated.length) return 'undated';
  const days = Math.ceil((dated[0].getTime() - Date.now()) / 86_400_000);
  if (days < 0) return `${Math.abs(days)}d overdue`;
  if (days === 0) return 'today';
  if (days === 1) return 'tomorrow';
  return `in ${days} days`;
}
