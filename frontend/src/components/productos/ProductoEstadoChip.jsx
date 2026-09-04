export default function ProductoEstadoChip({ activo, nivelAlerta }) {
  if (!activo) {
    return <span className="fac-estado">Inactivo</span>;
  }
  if (nivelAlerta === 'CRITICO') {
    return <span className="fac-estado fac-estado-warn">Crítico</span>;
  }
  if (nivelAlerta === 'MINIMO') {
    return <span className="fac-estado fac-estado-warn">Mínimo</span>;
  }
  return <span className="fac-estado fac-estado-ok">Activo</span>;
}
