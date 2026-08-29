import React, { useEffect, useState } from 'react';
import { AppShell } from '../components/layout/AppShell';
import { BrainChat } from '../components/brain/BrainChat';
import { brainApi } from '../api/brainApi';
import type { BrainConversation } from '../types/Brain';
import { History, Clock, Plus, PanelLeftClose, PanelLeft } from 'lucide-react';

const formatRelativeTime = (dateStr?: string) => {
  if (!dateStr) return '';
  try {
    const diffMs = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return days === 1 ? '1d ago' : `${days}d ago`;
  } catch {
    return '';
  }
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
    <AppShell title="Cortex Brain" subtitle="Ask questions about your synced Gmail — answers cite real messages" noScroll>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: historyOpen ? '240px minmax(0, 1fr)' : 'minmax(0, 1fr)',
          height: 'calc(100dvh - 180px)',
          minHeight: 420,
          border: '1px solid var(--v-hairline)',
          borderRadius: 20,
          overflow: 'hidden',
          background: 'var(--v-panel)',
          boxShadow: 'var(--v-lift-1)',
        }}
      >
        {historyOpen && (
          <aside
            style={{
              display: 'flex',
              flexDirection: 'column',
              borderRight: '1px solid var(--v-hairline)',
              background: 'var(--v-ground-2)',
              minWidth: 0,
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '14px 14px 10px',
                flexShrink: 0,
              }}
            >
              <History size={15} style={{ color: 'var(--v-navy)' }} />
              <span className="v-label" style={{ flex: 1, color: 'var(--v-navy)' }}>History</span>
              <button
                type="button"
                className="vbtn vbtn-bare"
                style={{ width: 28, height: 28, padding: 0 }}
                onClick={() => setHistoryOpen(false)}
                aria-label="Hide history"
              >
                <PanelLeftClose size={15} />
              </button>
            </div>

            <button
              type="button"
              onClick={() => setSelectedConversationId(null)}
              className="vbtn vbtn-signal"
              style={{ margin: '0 12px 10px', height: 36, fontSize: 12.5 }}
            >
              <Plus size={14} /> New conversation
            </button>

            <div className="v-scroll" style={{ flex: 1, overflowY: 'auto', padding: '0 10px 12px' }}>
              {conversations.length === 0 ? (
                <p style={{ fontSize: 12.5, color: 'var(--v-ink-3)', padding: '12px 6px', margin: 0, lineHeight: 1.45 }}>
                  No conversations yet. Ask about deadlines, placements, or today’s important mail.
                </p>
              ) : (
                conversations.map((conv) => {
                  const on = conv.id === selectedConversationId;
                  return (
                    <button
                      key={conv.id}
                      type="button"
                      onClick={() => setSelectedConversationId(conv.id)}
                      style={{
                        display: 'block',
                        width: '100%',
                        textAlign: 'left',
                        padding: '10px 12px',
                        marginBottom: 6,
                        borderRadius: 12,
                        border: on ? '1px solid var(--v-navy)' : '1px solid transparent',
                        background: on ? 'var(--v-navy-soft)' : 'var(--v-panel)',
                        cursor: 'pointer',
                      }}
                    >
                      <span
                        style={{
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical',
                          overflow: 'hidden',
                          fontSize: 12.5,
                          fontWeight: on ? 700 : 600,
                          color: 'var(--v-ink)',
                          lineHeight: 1.35,
                        }}
                      >
                        {conv.userQuery}
                      </span>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, marginTop: 6, fontSize: 10.5, color: 'var(--v-ink-3)' }}>
                        <Clock size={10} /> {formatRelativeTime(conv.createdAt)}
                      </span>
                    </button>
                  );
                })
              )}
            </div>
          </aside>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, minHeight: 0, position: 'relative' }}>
          {!historyOpen && (
            <button
              type="button"
              className="vbtn vbtn-bare"
              style={{ position: 'absolute', left: 10, top: 12, zIndex: 2, width: 32, height: 32, padding: 0 }}
              onClick={() => setHistoryOpen(true)}
              aria-label="Show history"
            >
              <PanelLeft size={16} />
            </button>
          )}
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
