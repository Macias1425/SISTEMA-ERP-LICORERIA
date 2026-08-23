import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import Button from './Button';

const enlaces = [
  { to: '/dashboard', label: 'Dashboard', roles: ['ADMIN', 'CAJERO', 'ALMACENISTA'] },
  { to: '/inventario', label: 'Inventario', roles: ['ADMIN', 'ALMACENISTA'] },
  { to: '/pos', label: 'Caja / POS', roles: ['ADMIN', 'CAJERO'] },
];

const etiquetaRol = {
  ADMIN: 'Administrador',
  CAJERO: 'Cajero',
  ALMACENISTA: 'Almacenista',
};

export default function Sidebar() {
  const { usuario, tieneRol, logout } = useAuth();
  const navigate = useNavigate();
  const visibles = enlaces.filter((enlace) => tieneRol(...enlace.roles));

  return (
    <aside className="sidebar">
      <div className="brand">POS LICORERÍA</div>
      <nav className="nav">
        {visibles.map((link) => (
          <NavLink key={link.to} to={link.to}>
            {link.label}
          </NavLink>
        ))}
      </nav>
      <div className="session-box">
        <strong>{usuario?.nombreCompleto}</strong>
        <span>{etiquetaRol[usuario?.rol] || usuario?.rol}</span>
        <Button
          variant="secondary"
          onClick={() => {
            logout();
            navigate('/login', { replace: true });
          }}
        >
          Cerrar sesión
        </Button>
      </div>
    </aside>
  );
}
