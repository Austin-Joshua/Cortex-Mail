import React from 'react';
import type { BrainMessage } from '../../types/Brain';
import { BrainMessageComponent } from './BrainMessage';
import { BrainInput } from './BrainInput';
import { useBrain } from '../../hooks/useBrain';
import { useAuthStore } from '../../store/authStore';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { brainApi } from '../../api/brainApi';
import { Brain, RotateCcw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const SUGGESTED_QUERIES: Record<string, string[]> = {
  STUDENT: [
    'What assignments are due this week?',
    'Any hackathons I should register for?',
    "What's my attendance status?",
    'Any placement opportunities?',
    'Summarize today\'s important emails',
  ],
  PROFESSOR: [
    'Any student queries I haven\'t responded to?',
    'What meetings do I have coming up?',
    'Summarize my research collaboration emails',
  ],
  DEFAULT: [
    'What are my most important emails today?',
    'Any upcoming deadlines I should know about?',
    'Summarize my recent communications',
  ],
};

interface BrainChatProps {
  selectedConversationId: number | null;
  setSelectedConversationId: (id: number | null) => void;
  onNewConversationSaved: () => void;
}

export const BrainChat: React.FC<BrainChatProps> = ({
  selectedConversationId,
  setSelectedConversationId,
  onNewConversationSaved,
}) => {
  const { messages, isLoading, sendQuery, clearMessages } = useBrain();
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const bottomRef = React.useRef<HTMLDivElement>(null);

  const { data: history = [] } = useQuery({
    queryKey: ['brain-history'],
    queryFn: brainApi.getHistory,
    staleTime: 60_000,
  });

  const displayedMessages = React.useMemo(() => {
    if (selectedConversationId) {
      const conv = history.find((c) => c.id === selectedConversationId);
      if (conv) {
        return [
          {
            id: `h-user-${conv.id}`,
            type: 'user' as const,
            content: conv.userQuery,
            timestamp: conv.createdAt ? new Date(conv.createdAt) : new Date(),
          },
          {
            id: `h-ai-${conv.id}`,
            type: 'assistant' as const,
            content: conv.aiResponse,
            timestamp: conv.createdAt ? new Date(conv.createdAt) : new Date(),
          },
        ];
      }
    }
    return messages;
  }, [selectedConversationId, history, messages]);

  React.useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [displayedMessages]);

  React.useEffect(() => {
    if (messages.length > 0 && messages.length % 2 === 0) {
      queryClient.invalidateQueries({ queryKey: ['brain-history'] });
      onNewConversationSaved();
    }
  }, [messages.length, queryClient]);

  const handleSend = (query: string) => {
    if (selectedConversationId) {
      setSelectedConversationId(null);
      clearMessages();
    }
    sendQuery(query);
  };

  const handleClear = () => {
    setSelectedConversationId(null);
    clearMessages();
  };

  const suggestions = SUGGESTED_QUERIES[user?.userRole ?? 'DEFAULT'] ?? SUGGESTED_QUERIES.DEFAULT;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: 'var(--bg)' }}>
      <div className="brain-header">
        <div className="brain-header-icon">
          <Brain size={18} />
        </div>
        <div style={{ flex: 1 }}>
          <p style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-1)', margin: 0, fontFamily: 'Google Sans, Roboto, sans-serif' }}>
            {selectedConversationId ? 'Archive View' : 'Cortex Brain'}
          </p>
          <p style={{ fontSize: 12, color: 'var(--text-2)', margin: '2px 0 0' }}>
            {selectedConversationId ? 'Viewing past conversation thread' : 'Natural language Q&A over your entire inbox'}
          </p>
        </div>
        {(displayedMessages.length > 0 || selectedConversationId) && (
          <button
            onClick={handleClear}
            className="btn-outline"
            style={{ padding: '4px 10px', fontSize: 11 }}
            title="Clear conversation"
          >
            <RotateCcw size={12} /> Clear
          </button>
        )}
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        {displayedMessages.length === 0 ? (
          <WelcomeState suggestions={suggestions} onSend={handleSend} />
        ) : (
          <div style={{ padding: 20, display: 'flex', flexDirection: 'column', gap: 16, maxWidth: 900, margin: '0 auto' }}>
            {displayedMessages.map((msg: BrainMessage) => (
              <BrainMessageComponent key={msg.id} message={msg} onEmailClick={(id) => navigate(`/inbox?emailId=${id}`)} />
            ))}
            {isLoading && <TypingIndicator />}
            <div ref={bottomRef} />
          </div>
        )}
      </div>

      <div
        style={{
          padding: '16px 20px',
          borderTop: '1px solid var(--border)',
          flexShrink: 0,
          background: 'var(--bg)',
        }}
      >
        <div style={{ maxWidth: 900, margin: '0 auto' }}>
          <BrainInput onSend={handleSend} isLoading={isLoading} />
        </div>
      </div>
    </div>
  );
};

const WelcomeState: React.FC<{ suggestions: string[]; onSend: (q: string) => void }> = ({
  suggestions,
  onSend,
}) => (
  <div
    style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      padding: 'clamp(24px, 4vh, 40px) clamp(20px, 4vw, 40px)',
      gap: 28,
      maxWidth: 880,
      margin: '0 auto',
      width: '100%',
      boxSizing: 'border-box',
    }}
    className="animate-fade-in"
  >
    <div style={{ textAlign: 'left' }}>
      <div
        style={{
          width: 48,
          height: 48,
          borderRadius: 14,
          background: 'var(--color-cortex-soft)',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: 14,
        }}
      >
        <Brain size={24} style={{ color: 'var(--color-cortex-light)' }} />
      </div>
      <h3 style={{ fontSize: 24, fontWeight: 800, color: 'var(--v-ink)', margin: '0 0 8px', letterSpacing: '-0.03em' }}>
        Ask your inbox anything
      </h3>
      <p style={{ fontSize: 14.5, color: 'var(--v-ink-2)', margin: 0, maxWidth: '52ch', lineHeight: 1.55 }}>
        Cortex Brain answers from your <strong>synced Gmail</strong> only. Sync the dashboard first if Inbox is empty.
      </p>
    </div>

    <div>
      <p className="v-label" style={{ marginBottom: 12, color: 'var(--color-cortex-light)' }}>Try asking</p>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: 10,
        }}
      >
        {suggestions.map((q) => (
          <button
            key={q}
            type="button"
            onClick={() => onSend(q)}
            className="brain-suggestion-card"
          >
            {q}
          </button>
        ))}
      </div>
    </div>
  </div>
);

const TypingIndicator: React.FC = () => (
  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
    <div className="brain-header-icon" style={{ width: 32, height: 32, borderRadius: '50%' }}>
      <Brain size={16} />
    </div>
    <div className="brain-ai-bubble" style={{ padding: '10px 14px', fontSize: 13, color: 'var(--color-text-secondary)' }}>
      Thinking...
    </div>
  </div>
);
