import React from 'react';
import { Clock } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { Placeholder } from '../components/bento/Placeholder';

export const ScheduledEmailsPage: React.FC = () => (
  <AppShell title="Scheduled" subtitle="Mail waiting to go out">
    <Placeholder
      icon={<Clock size={26} />}
      tone="var(--v-amber)"
      headline="Nothing queued"
      body="Write a reply now and choose when it leaves. Scheduled mail sits here until it sends, and you can edit or cancel it up to the moment it goes."
      points={['Pick a send time', 'Edit before it sends', 'Cancel anytime']}
    />
  </AppShell>
);
