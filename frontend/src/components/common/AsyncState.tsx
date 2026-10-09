import type { ReactNode } from 'react';

interface AsyncStateProps {
  loading: boolean;
  error: string | null;
  onRetry?: () => void;
  isEmpty?: boolean;
  emptyMessage?: string;
  children: ReactNode;
}

export default function AsyncState({
  loading,
  error,
  onRetry,
  isEmpty = false,
  emptyMessage = 'Nada por aqui.',
  children,
}: AsyncStateProps) {
  if (loading) {
    return (
      <p role="status" className="async-state__loading">
        Carregando...
      </p>
    );
  }

  if (error) {
    return (
      <div role="alert" className="async-state__error">
        <p className="error-message">{error}</p>
        {onRetry && (
          <button type="button" onClick={onRetry}>
            Tentar novamente
          </button>
        )}
      </div>
    );
  }

  if (isEmpty) {
    return emptyMessage ? (
      <p role="status" className="async-state__empty">
        {emptyMessage}
      </p>
    ) : null;
  }

  return <>{children}</>;
}
