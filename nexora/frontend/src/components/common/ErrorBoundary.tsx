import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle } from 'lucide-react';

interface Props { children: ReactNode; }
interface State { hasError: boolean; error?: Error; }

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          className="flex flex-col items-center justify-center min-h-screen gap-4 text-center p-8"
          style={{ background: 'var(--color-bg-app)', color: 'var(--color-text-primary)' }}
        >
          <div
            className="w-16 h-16 rounded-2xl flex items-center justify-center"
            style={{
              background: 'var(--color-danger-soft)',
              border: '1px solid var(--color-border)',
              color: 'var(--color-danger)',
            }}
          >
            <AlertTriangle size={28} />
          </div>
          <h2 className="text-xl font-semibold" style={{ color: 'var(--color-text-primary)' }}>
            Something went wrong
          </h2>
          <p className="text-sm max-w-sm" style={{ color: 'var(--color-text-muted)' }}>
            {this.state.error?.message}
          </p>
          <button className="btn-accent" onClick={() => window.location.reload()}>
            Reload Page
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
