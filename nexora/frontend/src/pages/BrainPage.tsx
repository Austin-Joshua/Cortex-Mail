import React, { useEffect, useState } from 'react';
import { AppShell } from '../components/layout/AppShell';
import { BrainChat } from '../components/brain/BrainChat';
import { brainApi } from '../api/brainApi';
import type { BrainConversation } from '../types/Brain';
import { History, Clock, ChevronLeft, ChevronRight, Plus } from 'lucide-react';

const formatRelativeTime = (dateStr?: string) => {
  if (!dateStr) return '';
  try {
    const diffMs = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return 'just now';
    if (mins === 1) return '1m ago';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 3600000);
    if (hrs === 1) return '1h ago';
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    if (days === 1) return '1d ago';
    return `${days}d ago`;
  } catch { return ''; }
};

export const BrainPage: React.FC = () => {
  const [historyOpen, setHistoryOpen] = useState(true);
  const [conversations, setConversations] = useState<BrainConversation[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<number | null>(null);

  const fetchHistory = () => {
    brainApi.getHistory().then(setConversations).catch(() => {});
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  return (
    <AppShell title="Velocity Brain" subtitle="Natural language Q&A over your emails" noScroll>
      <div style={{ display: 'flex', height: '100%', overflow: 'hidden', position: 'relative' }}>
        <div
          style={{
            flexShrink: 0,
            transition: 'width 0.25s ease',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
            width: historyOpen ? 220 : 0,
            borderRight: historyOpen ? '1px solid var(--border)' : 'none',
            background: 'var(--bg)',
          }}
        >
          {historyOpen && (
            <>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  padding: '10px 14px',
                  borderBottom: '1px solid var(--border)',
                  flexShrink: 0,
                }}
              >
                <History size={14} style={{ color: 'var(--accent)' }} />
                <span className="section-label" style={{ flex: 1 }}>HISTORY</span>
              </div>

              <button
                onClick={() => setSelectedConversationId(null)}
                className="btn-accent"
                style={{
                  margin: '8px 8px 4px',
                  padding: '6px 12px',
                  fontSize: 12,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 4,
                  cursor: 'pointer',
                }}
              >
                <Plus size={14} /> New conversation
              </button>

              <div style={{ flex: 1, overflowY: 'auto', padding: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
                {conversations.length === 0 ? (
                  <div style={{ textAlign: 'center', color: 'var(--text-3)', fontSize: 12, padding: 16 }}>No history yet</div>
                ) : conversations.map((conv) => {
                  const isSelected = conv.id === selectedConversationId;
                  return (
                    <div
                      key={conv.id}
                      onClick={() => setSelectedConversationId(conv.id)}
                      style={{
                        padding: '10px 12px',
                        borderRadius: 6,
                        background: isSelected ? 'var(--accent-soft)' : 'var(--bg)',
                        border: isSelected ? '1px solid var(--accent)' : '1px solid var(--border)',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                      }}
                    >
                      <p style={{
                        fontSize: 12,
                        fontWeight: isSelected ? 700 : 400,
                        color: isSelected ? 'var(--accent)' : 'var(--text-1)',
                        margin: '0 0 4px',
                        lineHeight: 1.4,
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden'
                      }}>
                        {conv.userQuery}
                      </p>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--text-3)', fontSize: 10 }}>
                        <Clock size={10} />
                        <span>{formatRelativeTime(conv.createdAt)}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </div>

        <button
          onClick={() => setHistoryOpen(o => !o)}
          style={{
            position: 'absolute',
            left: historyOpen ? 220 : 0,
            top: '50%',
            transform: 'translateY(-50%)',
            zIndex: 10,
            width: 16,
            height: 40,
            borderRadius: '0 6px 6px 0',
            background: 'var(--bg)',
            border: '1px solid var(--border)',
            borderLeft: 'none',
            color: 'var(--text-2)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'left 0.25s ease',
          }}
          title={historyOpen ? 'Close history' : 'Open history'}
        >
          {historyOpen ? <ChevronLeft size={12} /> : <ChevronRight size={12} />}
        </button>

        <div style={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
          <BrainChat 
            selectedConversationId={selectedConversationId} 
            setSelectedConversationId={setSelectedConversationId} 
            onNewConversationSaved={fetchHistory}
          />
        </div>
      </div>
    </AppShell>
  );
};
