import React from 'react';
import { Share2 } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Placeholder } from '../components/bento/Placeholder';

export const SharedPage: React.FC = () => (
  <AppShell title="Shared" subtitle="Threads you and your team are working on together">
    <Placeholder
      icon={<Share2 size={26} />}
      tone="var(--v-ink)"
      headline="Nothing shared yet"
      body="Share a thread to give teammates the full context — the messages, the extracted deadlines and the open actions — without forwarding anything."
      points={['Shared context', 'Comments in thread', 'Assigned actions']}
    />
  </AppShell>
);
