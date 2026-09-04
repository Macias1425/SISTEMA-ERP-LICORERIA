import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../services/api';
import { authService } from '../services/authService';
import { clearToken, getToken, setToken } from './session';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setCargando(false);
      return;
    }

    authService.me()
      .then(setUsuario)
      .catch(() => {
        clearToken();
        setUsuario(null);
      })
      .finally(() => setCargando(false));
  }, []);

  const value = useMemo(() => ({
    usuario,
    cargando,
    autenticado: Boolean(usuario),
    debeCambiarPassword: Boolean(usuario?.debeCambiarPassword),
    tieneRol: (...roles) => Boolean(usuario && roles.includes(usuario.rol)),
    tienePermiso: (...permisos) => Boolean(
      usuario?.permisosEfectivos?.some((p) => permisos.includes(p))
      || usuario?.rol === 'ADMIN'
    ),

    async login(username, password) {
      clearToken();
      const respuesta = await authService.login({ username, password });
      setToken(respuesta.token);
      const perfil = await authService.me();
      setUsuario(perfil);
      return perfil;
    },

    async cambiarPassword(passwordActual, passwordNueva, confirmacion) {
      await authService.cambiarPassword({ passwordActual, passwordNueva, confirmacion });
      const perfil = await authService.me();
      setUsuario(perfil);
      return perfil;
    },

    logout() {
      clearToken();
      setUsuario(null);
    },
  }), [usuario, cargando]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
}

export function mensajeError(error) {
  if (error instanceof ApiError) {
    const texto = error.mensaje || error.message || 'Ocurrió un error';
    return error.codigo && error.codigo !== 'ERROR'
      ? `${texto} (${error.codigo})`
      : texto;
  }
  return error?.message || 'Ocurrió un error';
}
