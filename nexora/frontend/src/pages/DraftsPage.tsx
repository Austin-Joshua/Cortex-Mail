import React from 'react';
import { useNavigate } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Placeholder } from '../components/bento/Placeholder';

export const DraftsPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <AppShell title="Drafts" subtitle="Unsent mail, saved as you type">
      <Placeholder
        icon={<FileText size={26} />}
        headline="No drafts yet"
        body="Anything you start writing is saved here automatically, so you can leave a reply half-finished and pick it up on another device."
        points={['Autosaved', 'Schedule a send', 'Reuse a template']}
        action={{ label: 'Ask Velocity Brain', onClick: () => navigate('/brain') }}
      />
    </AppShell>
  );
};
