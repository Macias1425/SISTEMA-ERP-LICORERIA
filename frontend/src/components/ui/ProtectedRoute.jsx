import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

export function RequireAuth() {
  const { autenticado, cargando, debeCambiarPassword } = useAuth();
  const location = useLocation();

  if (cargando) {
    return <div className="auth-loading">Cargando sesión…</div>;
  }
  if (!autenticado) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (debeCambiarPassword && location.pathname !== '/cambiar-password') {
    return <Navigate to="/cambiar-password" replace />;
  }
  return <Outlet />;
}

export function RequireGuest() {
  const { autenticado, cargando, debeCambiarPassword } = useAuth();

  if (cargando) {
    return <div className="auth-loading">Cargando sesión…</div>;
  }
  if (autenticado && debeCambiarPassword) {
    return <Navigate to="/cambiar-password" replace />;
  }
  if (autenticado) {
    return <Navigate to="/dashboard" replace />;
  }
  return <Outlet />;
}

export function RequireRole({ roles }) {
  const { tieneRol } = useAuth();
  if (!tieneRol(...roles)) {
    return <Navigate to="/dashboard" replace />;
  }
  return <Outlet />;
}
