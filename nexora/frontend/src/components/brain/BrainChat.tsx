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
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          padding: '12px 20px',
          borderBottom: '1px solid var(--border)',
          flexShrink: 0,
        }}
      >
        <div
          style={{
            width: 34,
            height: 34,
            borderRadius: 8,
            background: 'var(--accent-soft)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <Brain size={18} style={{ color: 'var(--accent)' }} />
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
      alignItems: 'center',
      justifyContent: 'center',
      height: '100%',
      padding: '32px 24px',
      gap: 24,
      textAlign: 'center',
    }}
    className="animate-fade-in"
  >
    <div>
      <div
        style={{
          width: 56,
          height: 56,
          borderRadius: '50%',
          background: 'var(--accent-soft)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 16px',
        }}
      >
        <Brain size={28} style={{ color: 'var(--accent)' }} />
      </div>
      <h3 style={{ fontSize: 22, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 8px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
        Ask your inbox anything
      </h3>
      <p style={{ fontSize: 14, color: 'var(--text-2)', margin: 0, maxWidth: 360, lineHeight: 1.5 }}>
        Cortex Brain reads all your emails and answers in plain English.
      </p>
    </div>

    <div style={{ width: '100%', maxWidth: 460 }}>
      <p className="section-label" style={{ marginBottom: 12 }}>TRY ASKING...</p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {suggestions.map((q, i) => (
          <button
            key={i}
            onClick={() => onSend(q)}
            style={{
              width: '100%',
              textAlign: 'left',
              padding: '10px 14px',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 8,
              fontSize: 13,
              color: 'var(--text-1)',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
          >
            → {q}
          </button>
        ))}
      </div>
    </div>
  </div>
);

const TypingIndicator: React.FC = () => (
  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
    <div
      style={{
        width: 32,
        height: 32,
        borderRadius: '50%',
        background: 'var(--accent-soft)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
      }}
    >
      <Brain size={16} style={{ color: 'var(--accent)' }} />
    </div>
    <div
      style={{
        padding: '10px 14px',
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        fontSize: 13,
        color: 'var(--text-2)',
      }}
    >
      Thinking...
    </div>
  </div>
);
