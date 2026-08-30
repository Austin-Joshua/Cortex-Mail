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
import { CAT_COLORS, CATEGORY_LABELS } from '../utils/catColors';
import { BarChart2, Users, Mail, Sparkles } from 'lucide-react';
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

export const AnalyticsPage: React.FC = () => {
  const navigate = useNavigate();

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: queryKeys.dashboardSummary,
    queryFn: dashboardApi.getSummary,
    staleTime: 120_000,
  });

  const { data: emailPage, isLoading: emailsLoading } = useQuery({
    queryKey: queryKeys.analyticsEmails,
    queryFn: () => emailApi.getEmails({ size: 100 }),
    staleTime: 120_000,
  });

  const { data: senders, isLoading: sendersLoading } = useQuery({
    queryKey: queryKeys.senders,
    queryFn: emailApi.getSenderSummary,
    staleTime: 120_000,
  });

  const { data: volumeHistory, isLoading: volumeLoading } = useQuery({
    queryKey: queryKeys.emailVolume(7),
    queryFn: () => dashboardApi.getEmailVolume(7),
    staleTime: 120_000,
  });

  const isLoading = summaryLoading || emailsLoading || sendersLoading || volumeLoading;

  const categoryData = useMemo(() => {
    if (!summary?.categoryCounts) return [];
    return Object.entries(summary.categoryCounts).map(([cat, count]) => ({
      name: CATEGORY_LABELS[cat] ?? cat,
      value: count as number,
      color: CAT_COLORS[cat]?.text ?? '#9AA6B2',
      raw: cat,
    }));
  }, [summary]);

  const formattedVolumeData = useMemo(() => {
    if (!volumeHistory) return [];
    return volumeHistory.map(pt => {
      try {
        const parts = pt.date.split('-');
        if (parts.length === 3) {
          const dateObj = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
          return {
            day: dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
            emails: pt.count,
          };
        }
      } catch {}
      return { day: pt.date, emails: pt.count };
    });
  }, [volumeHistory]);

  const totalEmails = emailPage?.totalElements ?? 0;
  const unreadCount = summary?.unreadCount ?? 0;

  if (isLoading) {
    return (
      <AppShell title="Analytics" subtitle="Insights into your email patterns">
        <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }} className="animate-fade-in">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
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
    <AppShell title="Analytics" subtitle="Insights into your email patterns and volumes">
      <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* ROW 1: 4 Stat Cards */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }} className="animate-fade-in">
          <StatCard label="Total Mail" value={totalEmails} accentColor="var(--accent)" icon={Mail} sub="synced overall" />
          <StatCard label="Unread Attention" value={unreadCount} accentColor="var(--danger)" icon={Sparkles} sub="pending read" />
          <StatCard label="Active Senders" value={senders?.length ?? 0} accentColor="var(--success)" icon={Users} sub="unique contacts" />
          <StatCard label="Categories" value={categoryData.length} accentColor="var(--star)" icon={BarChart2} sub="classes found" />
        </div>

        {/* ROW 2: Distribution & Volume */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 16 }}>
          {/* Category Pie Chart */}
          <div className="surface-elevated" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span className="section-label">CATEGORY DISTRIBUTION</span>
            </div>
            <div style={{ height: 200, position: 'relative' }}>
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
            </div>
          </div>

          {/* Email Volume over 7 days */}
          <div className="surface-elevated" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
            <span className="section-label">EMAIL VOLUME (LAST 7 DAYS)</span>
            <div style={{ height: 200 }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={formattedVolumeData} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="day" tick={{ fontSize: 11, fill: 'var(--text-2)' }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: 'var(--text-2)' }} axisLine={false} tickLine={false} allowDecimals={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Line
                    type="monotone"
                    dataKey="emails"
                    name="Emails"
                    stroke="var(--accent)"
                    strokeWidth={2}
                    dot={{ fill: 'var(--accent)', r: 3 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
};
