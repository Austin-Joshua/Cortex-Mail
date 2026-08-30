import React from 'react';
import { Inbox } from 'lucide-react';

interface EmptyStateProps {
  title: string;
  description?: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  icon,
  action,
}) => (
  <div className="flex flex-col items-center justify-center py-20 text-center animate-fade-in">
    <div
      className="w-16 h-16 rounded-2xl flex items-center justify-center mb-4"
      style={{
        background: 'var(--color-surface-elevated)',
        border: '1px solid var(--color-border)',
        color: 'var(--color-text-muted)',
      }}
    >
      {icon ?? <Inbox size={28} />}
    </div>
    <h3 className="text-lg font-semibold mb-1" style={{ color: 'var(--color-text-primary)' }}>{title}</h3>
    {description && (
      <p className="text-sm max-w-xs" style={{ color: 'var(--color-text-muted)' }}>{description}</p>
    )}
    {action && <div className="mt-4">{action}</div>}
  </div>
);
