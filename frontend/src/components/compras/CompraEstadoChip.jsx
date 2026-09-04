export default function CompraEstadoChip({ estado }) {
  if (estado === 'PENDIENTE') {
    return <span className="com-estado com-estado-pendiente">Pendiente</span>;
  }
  if (estado === 'PARCIAL') {
    return <span className="com-estado com-estado-parcial">Parcial</span>;
  }
  if (estado === 'RECIBIDA') {
    return <span className="com-estado com-estado-recibida">Recibida</span>;
  }
  if (estado === 'CERRADA') {
    return <span className="com-estado com-estado-cerrada">Cerrada</span>;
  }
  if (estado === 'ANULADA') {
    return <span className="com-estado com-estado-anulada">Anulada</span>;
  }
  return <span className="com-estado">{estado || '—'}</span>;
}
