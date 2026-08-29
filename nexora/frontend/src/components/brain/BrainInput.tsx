import React, { useState, useRef, useEffect } from 'react';
import { Send, Sparkles } from 'lucide-react';

interface BrainInputProps {
  onSend: (query: string) => void;
  isLoading: boolean;
}

export const BrainInput: React.FC<BrainInputProps> = ({ onSend, isLoading }) => {
  const [query, setQuery] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
    }
  }, [query]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim() || isLoading) return;
    onSend(query.trim());
    setQuery('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const canSend = query.trim().length > 0 && !isLoading;

  return (
    <form onSubmit={handleSubmit} style={{ position: 'relative' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-end',
          gap: 10,
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 24,
          padding: '8px 12px 8px 16px',
        }}
      >
        <Sparkles size={18} style={{ color: 'var(--accent)', flexShrink: 0, marginBottom: 8 }} />
        <textarea
          ref={textareaRef}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask Cortex Brain about your emails..."
          rows={1}
          disabled={isLoading}
          style={{
            flex: 1,
            background: 'transparent',
            border: 'none',
            outline: 'none',
            resize: 'none',
            fontSize: 14,
            color: 'var(--text-1)',
            maxHeight: 120,
            fontFamily: 'Google Sans, Roboto, sans-serif',
            padding: '6px 0',
          }}
        />
        <button
          type="submit"
          disabled={!canSend}
          style={{
            width: 36,
            height: 36,
            borderRadius: '50%',
            background: canSend ? 'var(--accent)' : 'var(--border)',
            border: 'none',
            color: '#ffffff',
            cursor: canSend ? 'pointer' : 'not-allowed',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            transition: 'background 0.15s ease',
          }}
        >
          <Send size={16} />
        </button>
      </div>
    </form>
  );
};
