/** Aviso cuando una acción está bloqueada por regla de negocio. */
export default function BlockedActionNote({ motivo }) {
  if (!motivo) {
    return null;
  }
  return (
    <p className="blocked-action-note" role="status">
      <span>{motivo}</span>
    </p>
  );
}
