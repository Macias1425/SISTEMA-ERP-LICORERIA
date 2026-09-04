import Icon from './Icon';

export default function EmptyState({
  icon = 'package',
  title,
  message,
  className = '',
  compact = false,
}) {
  return (
    <div className={`ui-empty${compact ? ' ui-empty-compact' : ''}${className ? ` ${className}` : ''}`}>
      <span className="ui-empty-icon" aria-hidden>
        <Icon name={icon} size={compact ? 24 : 32} strokeWidth={1.75} />
      </span>
      {title ? <strong>{title}</strong> : null}
      {message ? <p>{message}</p> : null}
    </div>
  );
}
