import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Archive } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Placeholder } from '../components/bento/Placeholder';

export const ArchivePage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <AppShell title="Archive" subtitle="Everything you have cleared out of the inbox">
      <Placeholder
        icon={<Archive size={26} />}
        tone="var(--v-ink-3)"
        headline="Archive is empty"
        body="Archived mail leaves your inbox but stays fully searchable here, along with any deadlines and actions that were extracted from it."
        points={['Still searchable', 'Keeps its category', 'Restore in one step']}
        action={{ label: 'Open inbox', onClick: () => navigate('/inbox') }}
      />
    </AppShell>
  );
};
