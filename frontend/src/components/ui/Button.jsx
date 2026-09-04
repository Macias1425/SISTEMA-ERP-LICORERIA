export default function Button({
  children,
  variant = 'primary',
  type = 'button',
  onClick,
  disabled = false,
  className = '',
  ...rest
}) {
  const base = variant === 'secondary' ? 'btn secondary' : variant === 'danger' ? 'btn danger' : 'btn';

  return (
    <button
      type={type}
      className={className ? `${base} ${className}` : base}
      onClick={onClick}
      disabled={disabled}
      {...rest}
    >
      {children}
    </button>
  );
}
