export default function Button({ children, variant = 'primary', type = 'button', onClick, disabled = false }) {
  const className = variant === 'secondary' ? 'btn secondary' : variant === 'danger' ? 'btn danger' : 'btn';

  return (
    <button type={type} className={className} onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}
