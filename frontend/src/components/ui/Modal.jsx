import { useEffect } from 'react';

export default function Modal({
  open,
  title,
  subtitle,
  onClose,
  children,
  footer,
  size = 'md',
}) {
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    function onKey(event) {
      if (event.key === 'Escape') {
        onClose?.();
      }
    }
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', onKey);
    };
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  return (
    <div className="modal-overlay" onClick={onClose} role="presentation">
      <div
        className={`modal-panel modal-${size}`}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <header className="modal-header">
          <div className="modal-head-text">
            {subtitle ? <p className="brand-kicker">{subtitle}</p> : null}
            <h2 id="modal-title">{title}</h2>
          </div>
          {onClose ? (
            <button type="button" className="modal-close" onClick={onClose} aria-label="Cerrar">
              ×
            </button>
          ) : null}
        </header>
        <div className="modal-body">{children}</div>
        {footer ? <footer className="modal-footer">{footer}</footer> : null}
      </div>
    </div>
  );
}
