export default function FacturaEstadoChip({ estado }) {
  if (estado === 'EMITIDA') {
    return <span className="fca-estado fca-estado-pagada">Pagada</span>;
  }
  if (estado === 'ANULADA') {
    return <span className="fca-estado fca-estado-anulada">Anulada</span>;
  }
  if (estado === 'DEV_PARCIAL' || estado === 'DEVOLUCION_PARCIAL') {
    return <span className="fca-estado fca-estado-parcial">Dev. parcial</span>;
  }
  return <span className="fca-estado">{estado || '—'}</span>;
}
