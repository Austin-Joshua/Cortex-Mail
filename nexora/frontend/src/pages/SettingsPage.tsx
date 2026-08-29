import React, { useState } from 'react';
import { AppShell } from '../components/layout/AppShell';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../store/authStore';
import { Shield, User, Zap, LogOut } from 'lucide-react';

const SECURITY_POINTS = [
  'Mailbox changes (read, star, archive, trash) only run when you click them in Cortex Mail',
  'Gmail tokens encrypted at rest with AES-256',
  'JWT sessions expire automatically after 24 hours',
  'Synced content stays in your Cortex Mail workspace — not sold or used for ads',
];

export const SettingsPage: React.FC = () => {
  const { user } = useAuthStore();
  const { updateProfile, handleLogout } = useAuth();
  const [activeTab, setActiveTab] = useState<'general' | 'ai' | 'privacy'>('general');
  const [calendarSyncEnabled, setCalendarSyncEnabled] = useState(user?.calendarSyncEnabled ?? true);

  const handleCalendarToggle = async (val: boolean) => {
    setCalendarSyncEnabled(val);
    await updateProfile({ calendarSyncEnabled: val });
  };

  return (
    <AppShell title="Settings" subtitle="Manage your Cortex Mail preferences">
      <div style={{ display: 'flex', height: '100%', overflow: 'hidden', background: 'var(--bg)' }}>
        <div
          style={{
            width: 200,
            flexShrink: 0,
            borderRight: '1px solid var(--border)',
            background: 'var(--bg)',
            padding: '16px 0',
            display: 'flex',
            flexDirection: 'column',
            gap: 4,
          }}
        >
          <button
            onClick={() => setActiveTab('general')}
            className={`gmail-nav-item${activeTab === 'general' ? ' active' : ''}`}
            style={{ height: 40, fontSize: 13 }}
          >
            <User size={16} /> General
          </button>

          <button
            onClick={() => setActiveTab('ai')}
            className={`gmail-nav-item${activeTab === 'ai' ? ' active' : ''}`}
            style={{ height: 40, fontSize: 13 }}
          >
            <Zap size={16} /> AI &amp; Sync
          </button>

          <button
            onClick={() => setActiveTab('privacy')}
            className={`gmail-nav-item${activeTab === 'privacy' ? ' active' : ''}`}
            style={{ height: 40, fontSize: 13 }}
          >
            <Shield size={16} /> Privacy &amp; Security
          </button>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: 24, maxWidth: 640 }}>
          {activeTab === 'general' && (
            <div className="surface-elevated animate-fade-in" style={{ padding: 20 }}>
              <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 16px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
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
                </div>
              </div>
            </div>
          )}

          {activeTab === 'ai' && (
            <div className="surface-elevated animate-fade-in" style={{ padding: 20 }}>
              <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 16px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
                AI &amp; Calendar Integrations
              </h3>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <input
                  id="calendar-sync-toggle"
                  type="checkbox"
                  checked={calendarSyncEnabled}
                  onChange={e => handleCalendarToggle(e.target.checked)}
                  style={{ width: 16, height: 16, accentColor: 'var(--accent)', cursor: 'pointer' }}
                />
                <div>
                  <label htmlFor="calendar-sync-toggle" style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-1)', cursor: 'pointer' }}>
                    Auto-add deadlines to Google Calendar
                  </label>
                  <p style={{ fontSize: 12, color: 'var(--text-2)', margin: '2px 0 0' }}>
                    Cortex Mail automatically exports detected email deadlines into Google Calendar.
                  </p>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'privacy' && (
            <div className="surface-elevated animate-fade-in" style={{ padding: 20 }}>
              <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 16px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
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
