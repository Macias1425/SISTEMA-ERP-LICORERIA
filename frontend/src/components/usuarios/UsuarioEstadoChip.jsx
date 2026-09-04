export default function UsuarioEstadoChip({ activo, debeCambiarPassword, turnoCajaAbierto }) {
  if (!activo) {
    return <span className="cat-badge cat-badge-muted">Bloqueado</span>;
  }
  return (
    <span className="fac-estado-wrap">
      <span className="cat-badge cat-badge-ok">Activo</span>
      {debeCambiarPassword ? <span className="cat-badge cat-badge-info">Clave temporal</span> : null}
      {turnoCajaAbierto ? <span className="cat-badge">Caja abierta</span> : null}
    </span>
  );
}
