import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '../api/dashboardApi';
import { AppShell } from '../components/layout/AppShell';
import { PriorityFeed } from '../components/dashboard/PriorityFeed';
import { DeadlineCard } from '../components/dashboard/DeadlineCard';
import { ActionItemList } from '../components/dashboard/ActionItemList';
import { StatCard } from '../components/common/StatCard';
import { EmailVolumeHeatmap } from '../components/dashboard/EmailVolumeHeatmap';
import { StreakCard } from '../components/dashboard/StreakCard';
import { DashboardReportModal } from '../components/dashboard/DashboardReportModal';
import { useAuthStore } from '../store/authStore';
import { useUIStore } from '../store/uiStore';
import { useEmails } from '../hooks/useEmails';
import { CAT_COLORS } from '../utils/catColors';
import { Mail, Clock, ListCheck, Tag, RefreshCw, Sparkles, User } from 'lucide-react';
import { emailApi } from '../api/emailApi';

export const DashboardPage: React.FC = () => {
  const { user } = useAuthStore();
  const { openReportModal } = useUIStore();
  const { sync, isSyncing } = useEmails();
  const navigate = useNavigate();

  const hasSynced = React.useRef(false);
  React.useEffect(() => {
    if (user && !user.lastSyncedAt && !hasSynced.current) {
      hasSynced.current = true;
      sync();
    }
  }, [user?.lastSyncedAt]);

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: dashboardApi.getSummary,
    staleTime: 300_000,
    refetchInterval: isSyncing ? 3000 : false,
  });

  const { data: senders = [] } = useQuery({
    queryKey: ['senders'],
    queryFn: emailApi.getSenderSummary,
  });

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const firstName = user?.name?.split(' ')[0] ?? 'there';

  const unreadCount = data?.unreadCount ?? 0;
  const deadlineCount = data?.upcomingDeadlines?.length ?? 0;
  const actionCount = data?.pendingActions?.length ?? 0;

  // Dynamic sentence summary
  const summarySentence = React.useMemo(() => {
    if (deadlineCount > 0 && actionCount > 0) {
      return `You have ${deadlineCount} deadline${deadlineCount > 1 ? 's' : ''} coming up and ${actionCount} pending action item${actionCount > 1 ? 's' : ''}.`;
    }
    if (unreadCount > 0) {
      return `You have ${unreadCount} unread message${unreadCount > 1 ? 's' : ''} requiring your review today.`;
    }
    return `Your inbox is up to date and organized by AI.`;
  }, [deadlineCount, actionCount, unreadCount]);

  return (
    <AppShell>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-gap)' }}>
        {/* Syncing Banner */}
        {isSyncing && (
          <div
            className="card-paper animate-spring-up"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              padding: '14px 20px',
              borderLeft: '4px solid var(--ember)',
              background: 'var(--ember-soft)',
            }}
          >
            <RefreshCw size={18} style={{ color: 'var(--ember)' }} className="animate-spin" />
            <span style={{ fontSize: 14, color: 'var(--text-1)', fontWeight: 600 }}>
              Syncing Gmail inbox with Cortex Mail AI intelligence...
            </span>
          </div>
        )}

        {isLoading ? (
          <DashboardSkeleton />
        ) : (
          <>
            {/* Bento Grid Row 1: Hero Card + Streak Card */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '2.5fr 1fr',
                gap: 'var(--spacing-gap)',
              }}
              className="animate-spring-up"
            >
              {/* Hero Card */}
              <div
                className="card-paper"
                style={{
                  padding: 28,
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  gap: 8,
                  background: 'linear-gradient(135deg, var(--paper-2), var(--paper))',
                  border: '1px solid var(--line-strong)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--ember)', fontWeight: 700, fontSize: 12 }}>
                  <Sparkles size={16} /> GMAIL INTELLIGENCE HIGHLIGHT
                </div>
                <h1
                  style={{
                    fontSize: 26,
                    fontWeight: 800,
                    color: 'var(--text-1)',
                    margin: 0,
                    fontFamily: 'Google Sans, Roboto, sans-serif',
                    letterSpacing: '-0.02em',
                  }}
                >
                  {greeting}, {firstName}
                </h1>
                <p style={{ fontSize: 15, color: 'var(--text-2)', margin: 0, lineHeight: 1.5 }}>
                  {summarySentence}
                </p>
              </div>

              {/* Streak Card */}
              <StreakCard />
            </div>

            {/* Bento Grid Row 2: 4 Stat Cards */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: 'var(--spacing-gap)',
              }}
              className="animate-spring-up"
            >
              <StatCard
                label="Unread Mail"
                value={unreadCount}
                accentColor="var(--danger)"
                icon={Mail}
                sub="require attention"
                onClick={() => navigate('/inbox?filter=unread')}
              />
              <StatCard
                label="Upcoming Deadlines"
                value={deadlineCount}
                accentColor="var(--warn)"
                icon={Clock}
                sub="next 7 days"
                onClick={() => navigate('/inbox?filter=deadlines')}
              />
              <StatCard
                label="Pending Actions"
                value={actionCount}
                accentColor="var(--ember)"
                icon={ListCheck}
                sub="action items"
                onClick={() => navigate('/inbox?filter=actions')}
              />
              <StatCard
                label="Categories"
                value={Object.keys(data?.categoryCounts ?? {}).length}
                accentColor="var(--violet)"
                icon={Tag}
                sub="classified topics"
              />
            </div>

            {/* Bento Grid Row 3: 12-Week Heatmap */}
            <div className="animate-spring-up">
              <EmailVolumeHeatmap />
            </div>

            {/* Bento Grid Row 4: Category Distribution */}
            {Object.keys(data?.categoryCounts ?? {}).length > 0 && (
              <div className="card-paper animate-spring-up" style={{ padding: 20 }}>
                <span className="section-label" style={{ marginBottom: 12, display: 'block' }}>
                  CATEGORY LABELS &amp; TOPICS
                </span>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                  {Object.entries(data?.categoryCounts ?? {}).map(([cat, count]) => {
                    const cfg = CAT_COLORS[cat];
                    const color = cfg?.color ?? 'var(--text-2)';
                    return (
                      <button
                        key={cat}
                        onClick={() => navigate(`/inbox?category=${cat}`)}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 8,
                          padding: '6px 16px',
                          background: color + '15',
                          border: `1px solid ${color}35`,
                          borderRadius: 'var(--r-full)',
                          color,
                          fontSize: 13,
                          fontWeight: 700,
                          cursor: 'pointer',
                          transition: 'var(--transition-spring)',
                        }}
                      >
                        {cfg?.label ?? cat}
                        <span style={{ fontSize: 11, opacity: 0.8 }}>({count as number})</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Bento Grid Row 5: Sender Spotlight Row */}
            {senders.length > 0 && (
              <div className="card-paper animate-spring-up" style={{ padding: 20 }}>
                <span className="section-label" style={{ marginBottom: 12, display: 'block' }}>
                  SENDER SPOTLIGHT (TOP CONTACTS)
                </span>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 16 }}>
                  {senders.slice(0, 3).map((sender) => (
                    <div
                      key={sender.senderEmail}
                      onClick={() => navigate(`/inbox?sender=${encodeURIComponent(sender.senderEmail)}`)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        padding: 12,
                        background: 'var(--paper-2)',
                        border: '1px solid var(--line)',
                        borderRadius: 'var(--r-md)',
                        cursor: 'pointer',
                        transition: 'var(--transition-smooth)',
                      }}
                    >
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          borderRadius: '50%',
                          background: 'var(--ember-soft)',
                          color: 'var(--ember)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontWeight: 800,
                          fontSize: 14,
                        }}
                      >
                        <User size={18} />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <p style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-1)', margin: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {sender.senderName || sender.senderEmail}
                        </p>
                        <p style={{ fontSize: 11, color: 'var(--text-2)', margin: '2px 0 0' }}>
                          {sender.emailCount} emails · {sender.latestSubject || '(no subject)'}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Bento Grid Row 6: Un-congested Priority Feed, Deadlines & Action Items */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
                gap: 'var(--spacing-gap)',
              }}
              className="animate-spring-up"
            >
              {/* Priority Feed */}
              <div className="card-paper" style={{ padding: 20 }}>
                <PriorityFeed emails={data?.priorityEmails ?? []} onEmailClick={(email) => openReportModal(email)} />
              </div>

              {/* Upcoming Deadlines */}
              <div className="card-paper" style={{ padding: 20, display: 'flex', flexDirection: 'column', gap: 12 }}>
                <span className="section-label">UPCOMING DEADLINES</span>
                {(data?.upcomingDeadlines?.length ?? 0) === 0 ? (
                  <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--text-3)', fontSize: 13 }}>
                    No deadlines detected in next 7 days
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {data?.upcomingDeadlines?.map((email: any) => (
                      <div key={email.id} onClick={() => openReportModal(email)} style={{ cursor: 'pointer' }}>
                        <DeadlineCard email={email} />
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Action Item List */}
              <div className="card-paper" style={{ padding: 20 }}>
                <ActionItemList actions={data?.pendingActions ?? []} />
              </div>
            </div>
          </>
        )}
      </div>

      {/* Interactive Detailed Report Modal */}
      <DashboardReportModal />
    </AppShell>
  );
};

const DashboardSkeleton: React.FC = () => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }} className="animate-spring-up">
    <div className="skeleton" style={{ height: 120 }} />
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
      {[...Array(4)].map((_, i) => (
        <div key={i} className="skeleton" style={{ height: 90 }} />
      ))}
    </div>
    <div className="skeleton" style={{ height: 140 }} />
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16 }}>
      {[...Array(3)].map((_, i) => (
        <div key={i} className="skeleton" style={{ height: 260 }} />
      ))}
    </div>
  </div>
);
