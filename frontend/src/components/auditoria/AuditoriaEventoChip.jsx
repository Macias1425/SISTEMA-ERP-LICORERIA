export default function AuditoriaEventoChip({ etiqueta, codigo }) {
  return (
    <div className="aud-evento">
      <span className="aud-evento-badge">{etiqueta || codigo || 'Evento'}</span>
      {codigo ? <small className="aud-evento-code">{codigo}</small> : null}
    </div>
  );
}
