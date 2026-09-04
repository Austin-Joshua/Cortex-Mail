import React, { useMemo, useState } from 'react';
import { HelpCircle, Search, ChevronDown, Keyboard, Sparkles, Zap, Mail } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Tile, TileHead } from '../components/bento/Tile';

const CATEGORIES = [
  {
    title: 'Getting started',
    icon: HelpCircle,
    tone: 'var(--v-ink)',
    items: [
      {
        q: 'How do I connect Gmail?',
        a: 'Sign in with Google. Cortex Mail syncs your mail and can apply mailbox actions you start in the app (read, star, archive, trash). It does not send mail for you.',
      },
      {
        q: 'Where is my data stored?',
        a: 'Your messages are stored in your own Cortex Mail workspace and your Google tokens are encrypted with AES-256-GCM at rest. Nothing is shared with third parties.',
      },
      {
        q: 'Which providers work?',
        a: 'Gmail today. Outlook and Yahoo are not supported yet.',
      },
      {
        q: 'How is this different from Gmail or Superhuman?',
        a: 'Gmail is the mailbox. Superhuman and Spark make reading faster. Cortex Mail is a next-action layer on your Gmail: a score that points at Flagged, dates, or follow-ups, and groups that follow this inbox — not a generic student or job profile.',
      },
    ],
  },
  {
    title: 'Your score',
    icon: Zap,
    tone: 'var(--v-red)',
    items: [
      {
        q: 'What is the Cortex Score?',
        a: 'It starts at 100 and is debited by unread, flagged, and starred mail in Gmail, plus real inbox follow-ups, overdue dates written in messages, and meetings due today. Home shows the next step. Trash, spam, drafts, and promo dates do not count.',
      },
      {
        q: 'What are flow zones?',
        a: 'Fixed bands across your working day. Deep Focus and Reflection mute notifications; Collaboration and Rapid Fire let them through.',
      },
      {
        q: 'How does grouping work?',
        a: 'Gmail labels win first. Then Cortex remembers how you already grouped that sender. Specialty tabs (Tasks, Events, Placement) only appear if this mailbox already has that kind of mail — or the sender is clearly academic and the message is a real assignment.',
      },
    ],
  },
  {
    title: 'Cortex Brain',
    icon: Sparkles,
    tone: 'var(--v-green)',
    items: [
      {
        q: 'What can I ask it?',
        a: 'Questions about your own inbox — "what did recruiters send last week", "which deadlines land before Friday". Answers link back to the messages they came from.',
      },
      {
        q: 'Does it read my whole mailbox?',
        a: 'It searches only the messages already synced into your workspace, and only when you ask it something.',
      },
    ],
  },
  {
    title: 'Keyboard',
    icon: Keyboard,
    tone: 'var(--v-ink-3)',
    items: [
      {
        q: 'Which shortcuts exist?',
        a: 'Press / to jump to search and Escape to close any open menu.',
      },
    ],
  },
];

export const HelpPage: React.FC = () => {
  const [term, setTerm] = useState('');
  const [open, setOpen] = useState<Set<string>>(new Set());

  const toggle = (key: string) =>
    setOpen((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });

  const results = useMemo(() => {
    const q = term.trim().toLowerCase();
    if (!q) return CATEGORIES;
    return CATEGORIES.map((c) => ({
      ...c,
      items: c.items.filter(
        (i) => i.q.toLowerCase().includes(q) || i.a.toLowerCase().includes(q),
      ),
    })).filter((c) => c.items.length > 0);
  }, [term]);

  return (
    <AppShell title="Help" subtitle="How Cortex Mail works">
      {/* Sits outside the grid so it keeps its natural height rather than
          stretching to the bento's minimum row. */}
      <div
        className="tile v-rise"
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          gap: 10,
          padding: '0 16px',
          height: 48,
          marginBottom: 'var(--v-gap)',
        }}
      >
        <Search size={17} style={{ color: 'var(--v-ink-3)', flexShrink: 0 }} />
        <input
          type="text"
          placeholder="Search help…"
          value={term}
          onChange={(e) => setTerm(e.target.value)}
          style={{
            flex: 1,
            minWidth: 0,
            border: 'none',
            outline: 'none',
            background: 'transparent',
            fontSize: 15,
            color: 'var(--v-ink)',
            fontFamily: 'inherit',
          }}
        />
      </div>

      <div className="bento">
        {results.length > 0 ? (
          results.map((cat, i) => {
            const Icon = cat.icon;
            return (
              <Tile key={cat.title} span={6} index={i + 1}>
                <TileHead label={cat.title} icon={<Icon size={17} />} tone={cat.tone} />

                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {cat.items.map((item, idx) => {
                    const key = `${cat.title}-${idx}`;
                    const isOpen = open.has(key);
                    return (
                      <div
                        key={key}
                        style={{
                          border: '1px solid var(--v-hairline)',
                          borderRadius: 'var(--v-r-chip)',
                          overflow: 'hidden',
                          background: isOpen ? 'var(--v-panel-2)' : 'transparent',
                          transition: 'background var(--v-fast)',
                        }}
                      >
                        <button
                          onClick={() => toggle(key)}
                          aria-expanded={isOpen}
                          style={{
                            width: '100%',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            gap: 12,
                            padding: '13px 14px',
                            background: 'transparent',
                            border: 'none',
                            cursor: 'pointer',
                            textAlign: 'left',
                            fontFamily: 'inherit',
                            fontSize: 13.5,
                            fontWeight: 700,
                            color: 'var(--v-ink)',
                          }}
                        >
                          {item.q}
                          <ChevronDown
                            size={17}
                            style={{
                              flexShrink: 0,
                              color: 'var(--v-ink-3)',
                              transform: isOpen ? 'rotate(180deg)' : 'none',
                              transition: 'transform var(--v-fast)',
                            }}
                          />
                        </button>
                        {isOpen && (
                          <p className="v-body" style={{ padding: '0 14px 14px' }}>
                            {item.a}
                          </p>
                        )}
                      </div>
                    );
                  })}
                </div>
              </Tile>
            );
          })
        ) : (
          <Tile span={12} index={1}>
            <p className="v-body" style={{ textAlign: 'center', padding: '28px 0' }}>
              Nothing matches “{term}”.
            </p>
          </Tile>
        )}

        <Tile span={12} feature index={9}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 14,
              flexWrap: 'wrap',
            }}
          >
            <span className="glyph glyph-lg"><Mail size={20} /></span>
            <div style={{ flex: 1, minWidth: 220 }}>
              <p className="v-title">Still stuck?</p>
              <p className="v-meta" style={{ marginTop: 3 }}>
                Send us the details and we will pick it up.
              </p>
            </div>
            <a
              href="mailto:support@Cortex Mail.app"
              className="vbtn vbtn-signal"
              style={{ textDecoration: 'none' }}
            >
              Email support
            </a>
          </div>
        </Tile>
      </div>
    </AppShell>
  );
};
