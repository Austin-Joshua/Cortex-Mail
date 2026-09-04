import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
  XAxis, YAxis, CartesianGrid,
  LineChart, Line,
} from 'recharts';
import { AppShell } from '../components/layout/AppShell';
import { emailApi } from '../api/emailApi';
import { queryKeys } from '../api/queryKeys';
import { dashboardApi } from '../api/dashboardApi';
import { CAT_COLORS, CATEGORY_LABELS, scoreToneFor } from '../utils/catColors';
import {
  BarChart2, Users, Mail, Sparkles, Star, Flag, Clock, CalendarClock, Inbox,
} from 'lucide-react';
import { StatCard } from '../components/common/StatCard';

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div
      style={{
        background: 'var(--bg)',
        border: '1px solid var(--border)',
        borderRadius: 6,
        padding: '6px 10px',
        fontSize: 12,
        boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
      }}
    >
      {label && <p style={{ color: 'var(--text-3)', margin: '0 0 4px' }}>{label}</p>}
      {payload.map((entry: any, i: number) => (
        <p key={i} style={{ color: entry.color ?? entry.fill ?? 'var(--accent)', margin: 0, fontWeight: 600 }}>
          {entry.name}: {entry.value}
        </p>
      ))}
    </div>
  );
};

function formatVolume(volumeHistory: { date: string; count: number }[] | undefined) {
  if (!volumeHistory) return [];
  return volumeHistory.map((pt) => {
    try {
      const parts = pt.date.split('-');
      if (parts.length === 3) {
        const dateObj = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
        return {
          day: dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
          emails: pt.count,
        };
      }
    } catch { /* keep raw date */ }
    return { day: pt.date, emails: pt.count };
  });
}

export const AnalyticsPage: React.FC = () => {
  const navigate = useNavigate();

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: queryKeys.dashboardSummary,
    queryFn: dashboardApi.getSummary,
    staleTime: 120_000,
  });

  const { data: senders, isLoading: sendersLoading } = useQuery({
    queryKey: queryKeys.senders,
    queryFn: emailApi.getSenderSummary,
    staleTime: 120_000,
  });

  const { data: volume7, isLoading: volume7Loading } = useQuery({
    queryKey: queryKeys.emailVolume(7),
    queryFn: () => dashboardApi.getEmailVolume(7),
    staleTime: 120_000,
  });

  const { data: volume30, isLoading: volume30Loading } = useQuery({
    queryKey: queryKeys.emailVolume(30),
    queryFn: () => dashboardApi.getEmailVolume(30),
    staleTime: 120_000,
  });

  const isLoading = summaryLoading || sendersLoading || volume7Loading || volume30Loading;

  const categoryData = useMemo(() => {
    if (!summary?.categoryCounts) return [];
    return Object.entries(summary.categoryCounts)
      .map(([cat, count]) => ({
        name: CATEGORY_LABELS[cat] ?? cat,
        value: count as number,
        color: CAT_COLORS[cat]?.text ?? '#9AA6B2',
        raw: cat,
      }))
      .filter((row) => row.value > 0)
      .sort((a, b) => b.value - a.value);
  }, [summary]);

  const categoryTotal = categoryData.reduce((s, r) => s + r.value, 0);
  const formatted7 = useMemo(() => formatVolume(volume7), [volume7]);
  const formatted30 = useMemo(() => formatVolume(volume30), [volume30]);

  const totalEmails = summary?.storedEmailCount ?? 0;
  const unreadCount = summary?.unreadCount ?? 0;
  const readCount = Math.max(0, totalEmails - unreadCount);
  const readPct = totalEmails > 0 ? Math.round((readCount / totalEmails) * 100) : 0;
  const weekTotal = formatted7.reduce((s, d) => s + d.emails, 0);
  const monthTotal = formatted30.reduce((s, d) => s + d.emails, 0);
  const labels = summary?.gmailLabelCounts;
  const score = summary?.cortexScore;
  const followUps = summary?.pendingActions?.length ?? 0;
  const meetings = summary?.todaysMeetings?.length ?? 0;
  const deadlines = summary?.upcomingDeadlines?.length ?? 0;
  const topSenders = (senders ?? []).slice(0, 8);

  const gmailTiles = [
    { key: 'INBOX', label: 'Inbox', view: 'PRIMARY' as const },
    { key: 'STARRED', label: 'Starred', view: 'STARRED' as const },
    { key: 'IMPORTANT', label: 'Flagged', view: 'IMPORTANT' as const },
    { key: 'CATEGORY_PROMOTIONS', label: 'Promotions', view: 'PROMOTIONS' as const },
    { key: 'CATEGORY_SOCIAL', label: 'Social', view: 'SOCIAL' as const },
    { key: 'CATEGORY_UPDATES', label: 'Updates', view: 'UPDATES' as const },
    { key: 'CATEGORY_FORUMS', label: 'Forums', view: 'FORUMS' as const },
    { key: 'DRAFT', label: 'Drafts', href: '/drafts' },
  ].map((tile) => ({
    ...tile,
    total: labels?.[tile.key]?.messagesTotal ?? 0,
    unread: labels?.[tile.key]?.messagesUnread ?? 0,
  })).filter((tile) => tile.total > 0 || tile.key === 'INBOX');

  if (isLoading) {
    return (
      <AppShell title="Analytics" subtitle="Insights from this mailbox">
        <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }} className="animate-fade-in">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 }}>
            {[...Array(4)].map((_, i) => <div key={i} className="skeleton" style={{ height: 90 }} />)}
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            {[...Array(2)].map((_, i) => <div key={i} className="skeleton" style={{ height: 260 }} />)}
          </div>
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Analytics" subtitle="Counts from your synced Gmail — nothing is invented">
      <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 20 }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 }} className="animate-fade-in">
          <StatCard label="Total Mail" value={totalEmails} accentColor="var(--accent)" icon={Mail} sub="stored from Gmail" />
          <StatCard label="Unread" value={unreadCount} accentColor="var(--danger)" icon={Inbox} sub={`${readPct}% of stored mail is read`} onClick={() => navigate('/inbox?view=UNREAD')} />
          <StatCard label="This week" value={weekTotal} accentColor="var(--success)" icon={BarChart2} sub={`${monthTotal} in the last 30 days`} />
          <StatCard label="Senders" value={senders?.length ?? 0} accentColor="var(--star)" icon={Users} sub="people in stored mail" />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 }}>
          <StatCard
            label="Cortex Score"
            value={score?.ready && score.score != null ? score.score : '—'}
            accentColor={scoreToneFor(score?.score ?? null, score?.ready === true)}
            icon={Sparkles}
            sub={score?.ready ? `${score.band} · ${score.nextAction ?? 'Inbox scored'}` : score?.statusMessage ?? 'Sync Gmail to score'}
            onClick={() => navigate('/dashboard')}
          />
          <StatCard label="Follow-ups" value={followUps} accentColor="var(--v-orange)" icon={Flag} sub="open inbox actions" onClick={() => navigate('/dashboard')} />
          <StatCard label="Meetings today" value={meetings} accentColor="var(--v-green)" icon={CalendarClock} sub="from meeting mail" onClick={() => navigate('/inbox?category=MEETING')} />
          <StatCard label="Dates this week" value={deadlines} accentColor="var(--v-red)" icon={Clock} sub="written in messages" onClick={() => navigate('/scheduled')} />
        </div>

        {gmailTiles.length > 0 && (
          <div className="surface-elevated" style={{ padding: 16 }}>
            <span className="section-label">GMAIL TABS</span>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: 10, marginTop: 12 }}>
              {gmailTiles.map((tile) => (
                <button
                  key={tile.key}
                  type="button"
                  className="vbtn vbtn-quiet"
                  style={{ height: 'auto', padding: '10px 12px', textAlign: 'left', justifyContent: 'flex-start' }}
                  onClick={() => navigate('href' in tile && tile.href ? tile.href : `/inbox?view=${tile.view}`)}
                >
                  <div style={{ fontSize: 18, fontWeight: 800 }}>{tile.total}</div>
                  <div className="v-meta">{tile.label}{tile.unread > 0 ? ` · ${tile.unread} unread` : ''}</div>
                </button>
              ))}
            </div>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
          <div className="surface-elevated" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
            <span className="section-label">HOW THIS INBOX IS GROUPED</span>
            <div style={{ height: 200, position: 'relative' }}>
              {categoryData.length === 0 ? (
                <p className="v-body">No groups yet. Sync Gmail and wait for classification.</p>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={categoryData}
                      cx="50%"
                      cy="50%"
                      innerRadius={50}
                      outerRadius={80}
                      paddingAngle={2}
                      dataKey="value"
                      onClick={(_data, index) => {
                        const item = categoryData[index];
                        if (item) navigate(`/inbox?category=${item.raw}`);
                      }}
                      style={{ cursor: 'pointer' }}
                    >
                      {categoryData.map((entry, i) => (
                        <Cell key={i} fill={entry.color} stroke="var(--bg)" strokeWidth={2} />
                      ))}
                    </Pie>
                    <Tooltip content={<CustomTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {categoryData.map((row) => {
                const pct = categoryTotal > 0 ? Math.round((row.value / categoryTotal) * 100) : 0;
                return (
                  <button
                    key={row.raw}
                    type="button"
                    onClick={() => navigate(`/inbox?category=${row.raw}`)}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 10, background: 'none', border: 'none',
                      color: 'inherit', cursor: 'pointer', padding: 0, fontFamily: 'inherit', textAlign: 'left',
                    }}
                  >
                    <span style={{ width: 8, height: 8, borderRadius: 99, background: row.color, flexShrink: 0 }} />
                    <span style={{ flex: 1, fontSize: 13 }}>{row.name}</span>
                    <span className="v-meta">{row.value} · {pct}%</span>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="surface-elevated" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
            <span className="section-label">MAIL VOLUME · 30 DAYS</span>
            <div style={{ height: 220 }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={formatted30} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="day" tick={{ fontSize: 10, fill: 'var(--text-2)' }} axisLine={false} tickLine={false} interval={4} />
                  <YAxis tick={{ fontSize: 11, fill: 'var(--text-2)' }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Line type="monotone" dataKey="emails" name="Emails" stroke="var(--accent)" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <p className="v-meta">{weekTotal} last 7 days · {monthTotal} last 30 days</p>
          </div>
        </div>

        {score?.ready && score.factors?.length ? (
          <div className="surface-elevated" style={{ padding: 16 }}>
            <span className="section-label">WHY THE SCORE IS {score.score}</span>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 10, marginTop: 12 }}>
              {score.factors.map((factor) => (
                <div key={factor.key} style={{ padding: '10px 12px', borderRadius: 10, background: 'var(--v-ground-2)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                    <span style={{ fontSize: 13, fontWeight: 700 }}>{factor.label}</span>
                    <span style={{ color: factor.points < 0 ? 'var(--v-red)' : 'var(--v-ink-3)', fontWeight: 700 }}>
                      {factor.points}
                    </span>
                  </div>
                  <div className="v-meta" style={{ marginTop: 4 }}>{factor.detail}</div>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        <div className="surface-elevated" style={{ padding: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
            <span className="section-label">TOP SENDERS</span>
            <Star size={14} style={{ color: 'var(--text-3)' }} />
          </div>
          {topSenders.length === 0 ? (
            <p className="v-body">No senders yet. Sync Gmail first.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {topSenders.map((s) => (
                <button
                  key={s.senderEmail}
                  type="button"
                  className="stream-row"
                  style={{ width: '100%', textAlign: 'left', cursor: 'pointer' }}
                  onClick={() => navigate(`/inbox?search=${encodeURIComponent(s.senderEmail)}`)}
                >
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div className="truncate" style={{ fontSize: 13, fontWeight: 700 }}>{s.senderName || s.senderEmail}</div>
                    <div className="v-meta truncate">{s.latestSubject || s.senderEmail}</div>
                  </div>
                  <span style={{ fontWeight: 800, fontVariantNumeric: 'tabular-nums' }}>{s.emailCount}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
};
