import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ArrowRight, Mail, Shield, Sparkles } from 'lucide-react';
import { BrandLogo } from '../components/common/BrandLogo';

const HIGHLIGHTS = [
  {
    icon: <Mail size={18} />,
    title: 'Your Gmail, organized',
    body: 'Cortex syncs your inbox and groups mail by what each message is about — for you, not a job title.',
  },
  {
    icon: <Sparkles size={18} />,
    title: 'Personal intelligence',
    body: 'Priority, summaries, and Cortex Brain learn from your mailbox patterns and content.',
  },
  {
    icon: <Shield size={18} />,
    title: 'You stay in control',
    body: 'Read, star, archive, and trash only when you act. Gmail remains the source of truth.',
  },
];

/** Short welcome after first Google sign-in — no profession / role picker. */
export const OnboardingPage: React.FC = () => {
  const navigate = useNavigate();
  const { updateProfile, user } = useAuth();
  const [isLoading, setIsLoading] = React.useState(false);

  React.useEffect(() => {
    if (user?.onboardingComplete) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  const firstName = user?.name?.split(' ')[0] ?? 'there';

  const handleContinue = async () => {
    setIsLoading(true);
    try {
      // Marks onboarding complete without choosing a profession profile.
      await updateProfile({});
      navigate('/dashboard', { replace: true });
    } catch {
      setIsLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100dvh',
        background: 'var(--v-ground-2)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'clamp(20px, 4vw, 40px)',
      }}
    >
      <div style={{ width: '100%', maxWidth: 520 }} className="animate-fade-in">
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 16 }}>
            <BrandLogo size={40} />
          </div>
          <h1
            style={{
              fontSize: 'clamp(22px, 4vw, 28px)',
              fontWeight: 800,
              letterSpacing: '-0.03em',
              color: 'var(--v-ink)',
              margin: '0 0 8px',
            }}
          >
            Welcome, {firstName}
          </h1>
          <p style={{ fontSize: 14, color: 'var(--v-ink-3)', margin: 0, lineHeight: 1.5 }}>
            Cortex Mail works the same for everyone — your inbox is classified from your mail, not a student or manager label.
          </p>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 24 }}>
          {HIGHLIGHTS.map((item) => (
            <div
              key={item.title}
              className="surface-elevated"
              style={{
                padding: '14px 16px',
                display: 'flex',
                gap: 12,
                alignItems: 'flex-start',
              }}
            >
              <span style={{ color: 'var(--v-signal)', marginTop: 2, flexShrink: 0 }}>{item.icon}</span>
              <div>
                <p style={{ margin: 0, fontWeight: 700, fontSize: 13, color: 'var(--v-ink)' }}>{item.title}</p>
                <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--v-ink-3)', lineHeight: 1.45 }}>{item.body}</p>
              </div>
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={() => void handleContinue()}
          disabled={isLoading}
          className="vbtn vbtn-primary"
          style={{
            width: '100%',
            height: 48,
            justifyContent: 'center',
            opacity: isLoading ? 0.6 : 1,
          }}
        >
          {isLoading ? 'Opening your inbox…' : 'Continue to Cortex Mail'}
          <ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
};
