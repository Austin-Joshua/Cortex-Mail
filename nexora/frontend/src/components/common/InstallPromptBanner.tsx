import React, { useEffect, useState } from 'react';
import { useUIStore } from '../../store/uiStore';
import { Download, X } from 'lucide-react';

export const InstallPromptBanner: React.FC = () => {
  const { installPrompt } = useUIStore();
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    const isDismissed = localStorage.getItem('nexora_pwa_dismissed');
    if (isDismissed) setDismissed(true);
  }, []);

  if (!installPrompt.isAvailable || dismissed || !installPrompt.deferredPrompt) return null;

  const handleInstall = async () => {
    try {
      await installPrompt.deferredPrompt.prompt();
      const choiceResult = await installPrompt.deferredPrompt.userChoice;
      if (choiceResult.outcome === 'accepted') {
        setDismissed(true);
      }
    } catch {}
  };

  const handleDismiss = () => {
    setDismissed(true);
    localStorage.setItem('nexora_pwa_dismissed', 'true');
  };

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 80,
        left: 20,
        right: 20,
        maxWidth: 400,
        margin: '0 auto',
        background: 'var(--ink-900)',
        color: '#ffffff',
        padding: '12px 16px',
        borderRadius: 'var(--r-md)',
        boxShadow: '0 8px 30px rgba(0, 0, 0, 0.4)',
        zIndex: 90,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        border: '1px solid var(--ember)',
      }}
      className="animate-spring-up"
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <Download size={20} style={{ color: 'var(--ember)' }} />
        <div>
          <p style={{ fontSize: 13, fontWeight: 700, margin: 0 }}>Install Velocity App</p>
          <p style={{ fontSize: 11, color: 'var(--text-3)', margin: '2px 0 0' }}>Add to your home screen</p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <button
          onClick={handleInstall}
          className="btn-ember"
          style={{ height: 32, padding: '0 12px', fontSize: 12 }}
        >
          Install
        </button>
        <button
          onClick={handleDismiss}
          style={{ background: 'none', border: 'none', color: 'var(--text-3)', cursor: 'pointer', padding: 4 }}
        >
          <X size={16} />
        </button>
      </div>
    </div>
  );
};
