import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '../api/dashboardApi';
import { AppShell } from '../components/layout/AppShell';
import { useAuthStore } from '../store/authStore';
import { useEmails } from '../hooks/useEmails';
import { AlertCircle, CheckCircle2, Clock, TrendingUp, Zap, Brain, Send } from 'lucide-react';

export const DashboardPageNew: React.FC = () => {
  const { user } = useAuthStore();
  const { isSyncing } = useEmails();
  const navigate = useNavigate();

  const { data } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: dashboardApi.getSummary,
    staleTime: 300_000,
  });

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const firstName = user?.name?.split(' ')[0] ?? 'there';

  const stats = [
    {
      icon: AlertCircle,
      label: 'Unread',
      value: data?.unreadCount ?? 0,
      color: '#ff6b35',
      trend: '-3%',
      action: () => navigate('/inbox'),
    },
    {
      icon: Clock,
      label: 'Deadlines',
      value: data?.upcomingDeadlines?.length ?? 0,
      color: '#00d4aa',
      action: () => navigate('/priority'),
    },
    {
      icon: CheckCircle2,
      label: 'Actions',
      value: data?.pendingActions?.length ?? 0,
      color: '#10b981',
    },
    {
      icon: TrendingUp,
      label: 'This Week',
      value: data?.weeklyEmailCount ?? 0,
      color: '#3b4fea',
    },
  ];

  return (
    <AppShell title={`${greeting}, ${firstName}!`} subtitle="Your email productivity dashboard">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>

        {/* Stats Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: 16,
        }}>
          {stats.map((stat, idx) => {
            const Icon = stat.icon;
            return (
              <div
                key={idx}
                onClick={stat.action}
                style={{
                  background: 'white',
                  border: '1px solid var(--border)',
                  borderRadius: 16,
                  padding: 20,
                  cursor: stat.action ? 'pointer' : 'default',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12,
                }}
                onMouseEnter={e => {
                  if (stat.action) {
                    (e.currentTarget as HTMLElement).style.borderColor = stat.color;
                    (e.currentTarget as HTMLElement).style.boxShadow = `0 4px 12px ${stat.color}20`;
                  }
                }}
                onMouseLeave={e => {
                  (e.currentTarget as HTMLElement).style.borderColor = 'var(--border)';
                  (e.currentTarget as HTMLElement).style.boxShadow = 'none';
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{
                    width: 44,
                    height: 44,
                    borderRadius: 12,
                    background: `${stat.color}15`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}>
                    <Icon size={22} style={{ color: stat.color }} />
                  </div>
                  {stat.trend && (
                    <span style={{
                      fontSize: 12,
                      fontWeight: 600,
                      color: stat.trend.startsWith('+') ? '#10b981' : '#ef4444',
                    }}>
                      {stat.trend}
                    </span>
                  )}
                </div>
                <div>
                  <p style={{
                    margin: 0,
                    fontSize: 12,
                    color: 'var(--text-3)',
                    fontWeight: 500,
                    textTransform: 'uppercase',
                    letterSpacing: '0.05em',
                  }}>
                    {stat.label}
                  </p>
                  <p style={{
                    margin: '4px 0 0',
                    fontSize: 28,
                    fontWeight: 700,
                    color: 'var(--text-1)',
                  }}>
                    {stat.value}
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        {/* Quick Actions */}
        <div>
          <h2 style={{
            fontSize: 20,
            fontWeight: 700,
            color: 'var(--text-1)',
            margin: '0 0 16px',
          }}>
            Quick Actions
          </h2>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: 12,
          }}>
            {[
              { icon: Zap, label: 'View Priority Inbox', path: '/priority', color: '#ff6b35' },
              { icon: Brain, label: 'Ask Nexora Brain', path: '/brain', color: '#3b4fea' },
              { icon: Send, label: 'Compose Email', action: 'compose', color: '#00d4aa' },
              { icon: TrendingUp, label: 'View Analytics', path: '/analytics', color: '#10b981' },
            ].map((action, idx) => {
              const Icon = action.icon;
              return (
                <button
                  key={idx}
                  onClick={() => action.path ? navigate(action.path) : null}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                    padding: '12px 16px',
                    border: '1px solid var(--border)',
                    borderRadius: 12,
                    background: 'white',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    fontSize: 14,
                    fontWeight: 600,
                    color: 'var(--text-1)',
                  }}
                  onMouseEnter={e => {
                    (e.currentTarget as HTMLElement).style.background = `${action.color}10`;
                    (e.currentTarget as HTMLElement).style.borderColor = action.color;
                  }}
                  onMouseLeave={e => {
                    (e.currentTarget as HTMLElement).style.background = 'white';
                    (e.currentTarget as HTMLElement).style.borderColor = 'var(--border)';
                  }}
                >
                  <Icon size={18} style={{ color: action.color }} />
                  {action.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Upcoming Deadlines */}
        {data?.upcomingDeadlines && data.upcomingDeadlines.length > 0 && (
          <div>
            <h2 style={{
              fontSize: 20,
              fontWeight: 700,
              color: 'var(--text-1)',
              margin: '0 0 16px',
            }}>
              Upcoming Deadlines
            </h2>
            <div style={{
              background: 'white',
              border: '1px solid var(--border)',
              borderRadius: 16,
              overflow: 'hidden',
            }}>
              {data.upcomingDeadlines.slice(0, 5).map((deadline: any, idx: number) => (
                <div
                  key={idx}
                  style={{
                    padding: '16px 20px',
                    borderBottom: idx < 4 ? '1px solid var(--border)' : 'none',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    cursor: 'pointer',
                    transition: 'background-color 0.2s ease',
                  }}
                  onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = 'var(--surface-2)'; }}
                  onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
                >
                  <div>
                    <p style={{ margin: 0, fontWeight: 600, color: 'var(--text-1)' }}>
                      {deadline.title}
                    </p>
                    <p style={{ margin: '4px 0 0', fontSize: 13, color: 'var(--text-3)' }}>
                      Due: {new Date(deadline.dueDate).toLocaleDateString()}
                    </p>
                  </div>
                  <div style={{
                    background: '#ff6b35',
                    color: 'white',
                    padding: '6px 12px',
                    borderRadius: 20,
                    fontSize: 12,
                    fontWeight: 600,
                  }}>
                    {Math.ceil((new Date(deadline.dueDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24))} days
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* AI Insights */}
        <div style={{
          background: 'linear-gradient(135deg, #3b4fea15 0%, #00d4aa15 100%)',
          border: '1px solid var(--border)',
          borderRadius: 16,
          padding: 24,
          display: 'flex',
          alignItems: 'center',
          gap: 16,
        }}>
          <div style={{
            width: 56,
            height: 56,
            borderRadius: 16,
            background: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}>
            <Brain size={28} style={{ color: 'white' }} />
          </div>
          <div>
            <h3 style={{ margin: 0, fontWeight: 700, color: 'var(--text-1)', fontSize: 16 }}>
              AI-Powered Insights
            </h3>
            <p style={{ margin: '8px 0 0', color: 'var(--text-2)', fontSize: 14 }}>
              You're 23% more responsive this week. Keep up the great work!
            </p>
          </div>
        </div>
      </div>
    </AppShell>
  );
};
