import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import { rutaInicioPorPermisos } from '../../auth/rolesInfo';

export function useAuthLogin() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function ingresar(credenciales) {
    const user = credenciales.username.trim();
    const pass = credenciales.password;
    if (!user || !pass) {
      return false;
    }
    setError('');
    setEnviando(true);
    try {
      const perfil = await login(user, pass);
      if (perfil.debeCambiarPassword) {
        navigate('/cambiar-password', { replace: true });
        return true;
      }
      const from = location.state?.from;
      const inicio = rutaInicioPorPermisos(perfil);
      const destino = from && from !== '/login' && from !== '/' && !(perfil.rol !== 'CAJERO' && from === '/pos')
        ? from
        : inicio;
      navigate(destino, { replace: true });
      return true;
    } catch (err) {
      setError(mensajeError(err));
      return false;
    } finally {
      setEnviando(false);
    }
  }

  return { ingresar, error, setError, enviando };
}
