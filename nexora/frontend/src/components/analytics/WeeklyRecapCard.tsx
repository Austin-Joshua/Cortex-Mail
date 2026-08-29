import React from 'react';
import { Share2, Award } from 'lucide-react';

interface RecapProps {
  totalVolume?: number;
  topCategory?: string;
  busiestSender?: string;
}

export const WeeklyRecapCard: React.FC<RecapProps> = ({
  totalVolume = 42,
  topCategory = 'ASSIGNMENT',
  busiestSender = 'Dr. Sarah Chen',
}) => {
  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: 'My Cortex Mail Weekly Email Recap',
        text: `Managed ${totalVolume} emails this week with Cortex Mail AI intelligence! Top category: ${topCategory}.`,
        url: window.location.href,
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(`Managed ${totalVolume} emails this week with Cortex Mail AI!`);
      alert('Recap summary copied to clipboard!');
    }
  };

  return (
    <div
      className="card-paper"
      style={{
        padding: 24,
        background: 'var(--v-panel)',
        border: '1px solid var(--v-hairline)',
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--v-ink)', fontWeight: 800, fontSize: 13 }}>
          <Award size={18} /> YOUR WEEKLY INBOX RECAP
        </div>
        <button onClick={handleShare} className="btn-outline" style={{ height: 32, padding: '0 12px', fontSize: 12 }}>
          <Share2 size={14} /> Share
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <div>
          <span style={{ fontSize: 11, color: 'var(--text-2)', textTransform: 'uppercase', fontWeight: 700 }}>PROCESSED</span>
          <p style={{ fontSize: 24, fontWeight: 900, color: 'var(--text-1)', margin: '4px 0 0', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
            {totalVolume} <span style={{ fontSize: 12, fontWeight: 500 }}>emails</span>
          </p>
        </div>

        <div>
          <span style={{ fontSize: 11, color: 'var(--text-2)', textTransform: 'uppercase', fontWeight: 700 }}>TOP CATEGORY</span>
          <p style={{ fontSize: 16, fontWeight: 800, color: 'var(--v-orange)', margin: '4px 0 0' }}>
            {topCategory}
          </p>
        </div>

        <div>
          <span style={{ fontSize: 11, color: 'var(--text-2)', textTransform: 'uppercase', fontWeight: 700 }}>BUSIEST CONTACT</span>
          <p style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-1)', margin: '4px 0 0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {busiestSender}
          </p>
        </div>
      </div>
    </div>
  );
};
