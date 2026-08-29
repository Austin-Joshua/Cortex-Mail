import React, { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { AppShell } from '../components/layout/AppShell';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../store/authStore';
import { emailApi } from '../api/emailApi';
import { ACCOUNT_ROLES, accountRoleLabel } from '../utils/accountRoles';
import type { UserRole } from '../types/User';
import { Shield, User, Zap, LogOut, RefreshCw, CheckCircle } from 'lucide-react';

const SECURITY_POINTS = [
  'Mailbox changes (read, star, archive, trash) only run when you click them in Cortex Mail',
  'Gmail tokens encrypted at rest with AES-256',
  'JWT sessions expire automatically after 24 hours',
  'Synced content stays in your Cortex Mail workspace — not sold or used for ads',
];

export const SettingsPage: React.FC = () => {
  const { user } = useAuthStore();
  const { updateProfile, handleLogout } = useAuth();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'general' | 'ai' | 'privacy'>('general');
  const [calendarSyncEnabled, setCalendarSyncEnabled] = useState(user?.calendarSyncEnabled ?? true);
  const [selectedRole, setSelectedRole] = useState<UserRole | null>(user?.userRole ?? null);
  const [roleSaving, setRoleSaving] = useState(false);
  const [reclassifying, setReclassifying] = useState(false);
  const [status, setStatus] = useState('');

  const handleCalendarToggle = async (val: boolean) => {
    setCalendarSyncEnabled(val);
    await updateProfile({ calendarSyncEnabled: val });
  };

  const handleRoleSave = async () => {
    if (!selectedRole || selectedRole === user?.userRole) return;
    setRoleSaving(true);
    setStatus('Updating account type and re-analyzing your inbox…');
    try {
      await updateProfile({ role: selectedRole });
      await queryClient.invalidateQueries({ queryKey: ['emails'] });
      await queryClient.invalidateQueries({ queryKey: ['email-categories'] });
      setStatus('Account preferences saved. Mail divisions are updating in the background.');
    } catch {
      setStatus('Could not update account type. Try again.');
    } finally {
      setRoleSaving(false);
    }
  };

  const handleReanalyze = async () => {
    setReclassifying(true);
    setStatus('Re-analyzing every inbox message with your current preferences…');
    try {
      const result = await emailApi.classifyInbox({ force: true });
      await queryClient.invalidateQueries({ queryKey: ['emails'] });
      await queryClient.invalidateQueries({ queryKey: ['email-categories'] });
      await queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setStatus(result.message || `Re-analyzed ${result.classified} messages.`);
    } catch {
      setStatus('Re-analysis failed. Check that the backend is running.');
    } finally {
      setReclassifying(false);
    }
  };

  return (
    <AppShell title="Settings" subtitle="Manage your Cortex Mail preferences">
      <div className="settings-layout">
        <nav className="settings-nav">
          <button
            type="button"
            onClick={() => setActiveTab('general')}
            className={`gmail-nav-item${activeTab === 'general' ? ' active' : ''}`}
            style={{ height: 40, fontSize: 13, border: 'none', background: 'transparent', cursor: 'pointer' }}
          >
            <User size={16} /> General
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('ai')}
            className={`gmail-nav-item${activeTab === 'ai' ? ' active' : ''}`}
            style={{ height: 40, fontSize: 13, border: 'none', background: 'transparent', cursor: 'pointer' }}
          >
            <Zap size={16} /> AI &amp; Sync
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('privacy')}
            className={`gmail-nav-item${activeTab === 'privacy' ? ' active' : ''}`}
            style={{ height: 40, fontSize: 13, border: 'none', background: 'transparent', cursor: 'pointer' }}
          >
            <Shield size={16} /> Privacy
          </button>
        </nav>

        <div className="settings-panel">
          {activeTab === 'general' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="surface-elevated animate-fade-in" style={{ padding: 20 }}>
                <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 16px' }}>
                  Account &amp; Profile
                </h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 20 }}>
                  <div
                    style={{
                      width: 56,
                      height: 56,
                      borderRadius: '50%',
                      background: 'var(--accent)',
                      color: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 20,
                      fontWeight: 700,
                    }}
                  >
                    {user?.name?.[0]?.toUpperCase() || 'U'}
                  </div>
                  <div>
                    <p style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: 0 }}>{user?.name}</p>
                    <p style={{ fontSize: 13, color: 'var(--text-2)', margin: '2px 0 0' }}>{user?.email}</p>
                    <p style={{ fontSize: 12, color: 'var(--text-3)', margin: '4px 0 0' }}>
                      Account type: {accountRoleLabel(user?.userRole)}
                    </p>
                  </div>
                </div>
              </div>

              <div className="surface-elevated animate-fade-in" style={{ padding: 20 }}>
                <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 8px' }}>
                  Mail divisions (account type)
                </h3>
                <p style={{ fontSize: 13, color: 'var(--text-2)', margin: '0 0 16px', lineHeight: 1.5 }}>
                  Cortex Mail classifies and groups your inbox based on this profile. Changing it re-analyzes all mail into the divisions that fit your role.
                </p>
                <div className="role-grid">
                  {ACCOUNT_ROLES.map(({ role, label, description }) => {
                    const active = selectedRole === role;
                    return (
                      <button
                        key={role}
                        type="button"
                        onClick={() => setSelectedRole(role)}
                        className="vbtn vbtn-quiet"
                        style={{
                          textAlign: 'left',
                          height: 'auto',
                          padding: 14,
                          border: active ? '2px solid var(--accent)' : '1px solid var(--border)',
                          background: active ? 'var(--v-panel-2, #f8fafc)' : 'var(--bg)',
                          position: 'relative',
                        }}
                      >
                        {active && (
                          <CheckCircle size={16} style={{ position: 'absolute', top: 10, right: 10, color: 'var(--accent)' }} />
                        )}
                        <div style={{ fontWeight: 700, fontSize: 13, color: 'var(--text-1)', marginBottom: 4 }}>{label}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-3)', lineHeight: 1.4 }}>{description}</div>
                      </button>
                    );
                  })}
                </div>
                <button
                  type="button"
                  className="vbtn vbtn-primary"
                  style={{ marginTop: 16 }}
                  disabled={roleSaving || !selectedRole || selectedRole === user?.userRole}
                  onClick={() => void handleRoleSave()}
                >
                  {roleSaving ? 'Saving…' : 'Save account type & re-classify mail'}
                </button>
              </div>
            </div>
          )}

          {activeTab === 'ai' && (
            <div className="surface-elevated animate-fade-in" style={{ padding: 20 }}>
              <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 16px' }}>
                AI &amp; Classification
              </h3>
              <p style={{ fontSize: 13, color: 'var(--text-2)', margin: '0 0 16px', lineHeight: 1.5 }}>
                Every synced message is analyzed for category, priority, summary, and deadlines using your account type ({accountRoleLabel(user?.userRole)}).
              </p>
              <button
                type="button"
                className="vbtn vbtn-quiet"
                style={{ marginBottom: 20, display: 'inline-flex', alignItems: 'center', gap: 8 }}
                disabled={reclassifying}
                onClick={() => void handleReanalyze()}
              >
                <RefreshCw size={15} className={reclassifying ? 'animate-spin' : undefined} />
                {reclassifying ? 'Re-analyzing…' : 'Re-analyze all inbox mail'}
              </button>

              <div style={{ display: 'flex', alignItems: 'center', gap: 12, paddingTop: 16, borderTop: '1px solid var(--border)' }}>
                <input
                  id="calendar-sync-toggle"
                  type="checkbox"
                  checked={calendarSyncEnabled}
                  onChange={(e) => void handleCalendarToggle(e.target.checked)}
                  style={{ width: 16, height: 16, accentColor: 'var(--accent)', cursor: 'pointer' }}
                />
                <div>
                  <label htmlFor="calendar-sync-toggle" style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)', cursor: 'pointer' }}>
                    Auto-add deadlines to Google Calendar
                  </label>
                  <p style={{ fontSize: 12, color: 'var(--text-2)', margin: '2px 0 0' }}>
                    Detected deadlines from classified mail are exported to Google Calendar.
                  </p>
                </div>
              </div>
            </div>
          )}

          {status && (
            <p style={{ fontSize: 13, color: 'var(--text-2)', marginTop: 12, padding: '10px 14px', background: 'var(--v-panel-2, #f1f5f9)', borderRadius: 10 }}>
              {status}
            </p>
          )}

          {activeTab === 'privacy' && (
            <div className="surface-elevated animate-fade-in" style={{ padding: 20 }}>
              <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 16px' }}>
                Privacy &amp; Security Standards
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 20 }}>
                {SECURITY_POINTS.map((pt, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 13, color: 'var(--text-1)' }}>
                    <span style={{ color: 'var(--success)', fontWeight: 700 }}>✓</span>
                    <span>{pt}</span>
                  </div>
                ))}
              </div>

              <button
                onClick={handleLogout}
                className="btn-outline"
                style={{ color: 'var(--danger)', borderColor: 'var(--danger)' }}
              >
                <LogOut size={14} /> Revoke access &amp; log out
              </button>
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
};
