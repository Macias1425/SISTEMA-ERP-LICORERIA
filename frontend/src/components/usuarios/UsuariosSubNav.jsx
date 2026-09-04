import { NavLink } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { PERMISOS } from '../../auth/permisos';

export default function UsuariosSubNav() {
  const { tienePermiso } = useAuth();

  return (
    <nav className="usr-subnav" aria-label="Secciones de usuarios">
      <NavLink to="/usuarios" end>Gestión</NavLink>
      {tienePermiso(PERMISOS.PERMISOS_GESTIONAR) ? (
        <NavLink to="/usuarios/permisos">Permisos y roles</NavLink>
      ) : null}
    </nav>
  );
}
