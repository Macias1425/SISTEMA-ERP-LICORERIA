const ETIQUETAS = {
  PENDIENTE: 'Pendiente',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
};

export default function MermaEstadoChip({ estado }) {
  const pendiente = estado === 'PENDIENTE';
  const aprobada = estado === 'APROBADA';
  let clase = ' fac-estado-warn';
  if (aprobada) {
    clase = ' fac-estado-ok';
  } else if (!pendiente) {
    clase = '';
  }
  return (
    <span className={`fac-estado${clase}`}>
      {ETIQUETAS[estado] || estado}
    </span>
  );
}
