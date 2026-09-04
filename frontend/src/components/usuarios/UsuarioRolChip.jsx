const ETIQUETAS = {
  ADMIN: 'Administrador',
  CAJERO: 'Cajero',
  ALMACENISTA: 'Almacenista',
};

export default function UsuarioRolChip({ rol }) {
  const admin = rol === 'ADMIN';
  const almacen = rol === 'ALMACENISTA';
  const clase = admin ? ' cat-badge-warn' : almacen ? ' cat-badge-info' : '';
  return (
    <span className={`cat-badge${clase}`}>
      {ETIQUETAS[rol] || rol}
    </span>
  );
}

export function etiquetaRol(rol) {
  return ETIQUETAS[rol] || rol;
}
