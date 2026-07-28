import React, { useState } from 'react';
import { AppShell } from '../components/layout/AppShell';
import { HelpCircle, Search, ChevronDown, Mail, Brain, Zap } from 'lucide-react';

const HELP_CATEGORIES = [
  {
    title: 'Getting Started',
    icon: HelpCircle,
    items: [
      { q: 'How do I connect my Gmail account?', a: 'Click "Connect Gmail Account" on the dashboard. Authorize Velocity to access your emails with read-only permissions.' },
      { q: 'Is my data secure?', a: 'Yes! We use AES-256 encryption and only access your emails for classification and analysis. Your data is never shared.' },
      { q: 'What email providers are supported?', a: 'Currently Gmail. We plan to support Outlook and Yahoo Mail soon.' },
    ],
  },
  {
    title: 'Features',
    icon: Zap,
    items: [
      { q: 'What is Priority Inbox?', a: 'AI learns your patterns and shows the most important emails first based on your past interactions.' },
      { q: 'How does scheduled delivery work?', a: 'Compose an email, click "Schedule", choose time/date. Velocity sends it at the perfect moment.' },
      { q: 'Can I use email templates?', a: 'Yes! Create templates for common responses and reuse them with merge fields.' },
    ],
  },
  {
    title: 'Brain Q&A',
    icon: Brain,
    items: [
      { q: 'What can I ask Velocity Brain?', a: 'Ask questions like "What meetings do I have with John?" or "Summarize my marketing emails".' },
      { q: 'How accurate is the AI?', a: 'We use Claude AI for 99%+ accuracy. Results are based on your actual emails.' },
    ],
  },
  {
    title: 'Keyboard Shortcuts',
    icon: Mail,
    items: [
      { q: 'List of all shortcuts?', a: 'Press ? to see keyboard shortcuts. / focuses search, g+i goes to inbox, g+b goes to brain.' },
    ],
  },
];

export const HelpPage: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedIndices, setExpandedIndices] = useState<Set<string>>(new Set());

  const toggleExpanded = (key: string) => {
    const newSet = new Set(expandedIndices);
    if (newSet.has(key)) {
      newSet.delete(key);
    } else {
      newSet.add(key);
    }
    setExpandedIndices(newSet);
  };

  const filteredCategories = HELP_CATEGORIES.map(cat => ({
    ...cat,
    items: cat.items.filter(
      item => item.q.toLowerCase().includes(searchTerm.toLowerCase()) ||
              item.a.toLowerCase().includes(searchTerm.toLowerCase())
    ),
  })).filter(cat => cat.items.length > 0);

  return (
    <AppShell title="Help & Support" subtitle="Get answers to your questions">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24, maxWidth: 800 }}>
        {/* Search */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '12px 16px',
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 12,
          animation: 'slideDown 0.3s ease-out',
        }}>
          <Search size={20} style={{ color: 'var(--text-3)' }} />
          <input
            type="text"
            placeholder="Search help..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              flex: 1,
              border: 'none',
              outline: 'none',
              fontSize: 14,
              background: 'transparent',
            }}
          />
        </div>

        {/* Categories */}
        {filteredCategories.length > 0 ? (
          filteredCategories.map((category, catIdx) => {
            const Icon = category.icon;
            return (
              <div key={catIdx} style={{ animation: `fadeIn 0.3s ease-out 0.${catIdx * 100}ms forwards` }}>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  marginBottom: 12,
                }}>
                  <div style={{
                    width: 40,
                    height: 40,
                    borderRadius: 10,
                    background: 'var(--primary-pale)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}>
                    <Icon size={20} style={{ color: 'var(--primary)' }} />
                  </div>
                  <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700 }}>
                    {category.title}
                  </h3>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {category.items.map((item, idx) => {
                    const key = `${catIdx}-${idx}`;
                    const isExpanded = expandedIndices.has(key);
                    return (
                      <div
                        key={key}
                        style={{
                          border: '1px solid var(--border)',
                          borderRadius: 12,
                          overflow: 'hidden',
                          transition: 'all 0.2s ease',
                        }}
                      >
                        <button
                          onClick={() => toggleExpanded(key)}
                          style={{
                            width: '100%',
                            padding: '16px',
                            background: isExpanded ? 'var(--surface-2)' : 'var(--surface)',
                            border: 'none',
                            cursor: 'pointer',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            gap: 12,
                            transition: 'all 0.2s ease',
                          }}
                        >
                          <span style={{
                            textAlign: 'left',
                            fontWeight: 600,
                            color: 'var(--text-1)',
                          }}>
                            {item.q}
                          </span>
                          <ChevronDown
                            size={20}
                            style={{
                              color: 'var(--text-3)',
                              transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
                              transition: 'transform 0.2s ease',
                              flexShrink: 0,
                            }}
                          />
                        </button>
                        {isExpanded && (
                          <div style={{
                            padding: '0 16px 16px',
                            color: 'var(--text-2)',
                            lineHeight: 1.6,
                            animation: 'slideDown 0.2s ease-out',
                          }}>
                            {item.a}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })
        ) : (
          <div style={{
            textAlign: 'center',
            padding: '40px 20px',
            color: 'var(--text-3)',
          }}>
            No results found. Try a different search term.
          </div>
        )}

        {/* Contact Support */}
        <div style={{
          background: 'rgba(59, 79, 234, 0.05)',
          border: '1px solid rgba(59, 79, 234, 0.2)',
          borderRadius: 16,
          padding: 24,
          textAlign: 'center',
          animation: 'fadeIn 0.3s ease-out 0.4s backwards',
        }}>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: 'var(--primary)', marginBottom: 8 }}>
            Still need help?
          </h3>
          <p style={{ margin: 0, color: 'var(--text-2)', marginBottom: 16 }}>
            Contact our support team at support@nexora.ai
          </p>
          <button style={{
            padding: '10px 20px',
            background: 'var(--primary)',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            transition: 'all 0.2s ease',
            boxShadow: '0 2px 6px rgba(59, 79, 234, 0.2)',
          }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.transform = 'scale(1.05)'; }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.transform = 'scale(1)'; }}
          >
            Send Email
          </button>
        </div>
      </div>
    </AppShell>
  );
};
