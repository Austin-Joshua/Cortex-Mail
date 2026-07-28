import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type { UserRole } from '../types/User';
import { GraduationCap, BookOpen, Monitor, Users, Briefcase, Globe2, ArrowRight, CheckCircle } from 'lucide-react';

interface RoleOption {
  role: UserRole;
  icon: React.ReactNode;
  label: string;
  description: string;
}

const ROLES: RoleOption[] = [
  { role: 'STUDENT',      icon: <GraduationCap size={20} />, label: 'Student',         description: 'Assignments, deadlines, placements & hackathons' },
  { role: 'PROFESSOR',    icon: <BookOpen size={20} />,       label: 'Professor',       description: 'Research, student queries & meeting management' },
  { role: 'IT_EMPLOYEE',  icon: <Monitor size={20} />,        label: 'IT Employee',     description: 'Support tickets, system alerts & tech comms' },
  { role: 'HR_PROFESSIONAL', icon: <Users size={20} />,       label: 'HR Professional', description: 'Recruitment, onboarding & policy communications' },
  { role: 'MANAGER',      icon: <Briefcase size={20} />,      label: 'Manager',         description: 'Team updates, approvals & project email tracking' },
  { role: 'FREELANCER',   icon: <Globe2 size={20} />,         label: 'Freelancer',      description: 'Client emails, invoices, deadlines & proposals' },
];

const ACCESS_POINTS = [
  'Reads your Gmail inbox (read-only OAuth scope)',
  'Stores AI summaries & action items — not raw emails',
  'Sends email content to AI for classification',
  'Never sends, modifies, or deletes your emails',
];

export const OnboardingPage: React.FC = () => {
  const navigate = useNavigate();
  const { updateRole, user } = useAuth();
  const [selectedRole, setSelectedRole] = React.useState<UserRole | null>(null);
  const [isLoading, setIsLoading] = React.useState(false);

  React.useEffect(() => {
    if (user?.onboardingComplete) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  const firstName = user?.name?.split(' ')[0] ?? 'there';

  const handleContinue = async () => {
    if (!selectedRole) return;
    setIsLoading(true);
    try {
      await updateRole(selectedRole);
      navigate('/dashboard');
    } catch {
      setIsLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'var(--bg)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 32,
      }}
    >
      <div style={{ width: '100%', maxWidth: 720 }} className="animate-fade-in">
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div
            style={{
              width: 44,
              height: 44,
              background: 'var(--accent)',
              borderRadius: 12,
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
              fontSize: 20,
              fontWeight: 800,
            }}
          >
            N
          </div>
          <h1 style={{ fontSize: 28, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 8px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
            Welcome, {firstName}
          </h1>
          <p style={{ fontSize: 14, color: 'var(--text-2)', margin: 0 }}>
            Choose your role to shape how Velocity prioritizes your emails
          </p>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 12,
            marginBottom: 24,
          }}
        >
          {ROLES.map(({ role, icon, label, description }) => {
            const isSelected = selectedRole === role;
            return (
              <button
                key={role}
                onClick={() => setSelectedRole(role)}
                style={{
                  background: isSelected ? 'var(--accent-soft)' : 'var(--bg)',
                  border: isSelected ? '2px solid var(--accent)' : '1px solid var(--border)',
                  borderRadius: 12,
                  padding: '16px',
                  textAlign: 'left',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                  position: 'relative',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                {isSelected && (
                  <div style={{ position: 'absolute', top: 12, right: 12 }}>
                    <CheckCircle size={18} style={{ color: 'var(--accent)' }} />
                  </div>
                )}
                <div style={{ color: isSelected ? 'var(--accent)' : 'var(--text-2)' }}>
                  {icon}
                </div>
                <p style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-1)', margin: 0 }}>
                  {label}
                </p>
                <p style={{ fontSize: 12, color: 'var(--text-2)', margin: 0, lineHeight: 1.4 }}>
                  {description}
                </p>
              </button>
            );
          })}
        </div>

        <div className="surface-elevated" style={{ padding: 16, marginBottom: 24 }}>
          <p className="section-label" style={{ marginBottom: 8 }}>WHAT NEXORA ACCESSES</p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 16px' }}>
            {ACCESS_POINTS.map(point => (
              <div key={point} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--text-2)' }}>
                <span style={{ color: 'var(--success)', fontWeight: 700 }}>✓</span>
                <span>{point}</span>
              </div>
            ))}
          </div>
        </div>

        <button
          onClick={handleContinue}
          disabled={!selectedRole || isLoading}
          className="btn-accent"
          style={{
            width: '100%',
            height: 48,
            justifyContent: 'center',
            fontSize: 15,
            opacity: (!selectedRole || isLoading) ? 0.4 : 1,
            cursor: (!selectedRole || isLoading) ? 'not-allowed' : 'pointer',
          }}
        >
          {isLoading ? 'Setting up dashboard...' : 'Continue to Dashboard'}
          <ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
};
