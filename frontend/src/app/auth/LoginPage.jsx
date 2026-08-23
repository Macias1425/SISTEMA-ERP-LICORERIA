import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import Button from '../../components/ui/Button';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function onSubmit(event) {
    event.preventDefault();
    setError('');
    setEnviando(true);
    try {
      const perfil = await login(username.trim(), password);
      if (perfil.debeCambiarPassword) {
        navigate('/cambiar-password', { replace: true });
        return;
      }
      const destino = location.state?.from && location.state.from !== '/login'
        ? location.state.from
        : '/dashboard';
      navigate(destino, { replace: true });
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={onSubmit}>
        <p className="auth-kicker">Sistema POS</p>
        <h1>Licorería</h1>
        <p className="auth-lead">Ingrese con su usuario de trabajo.</p>

        {error ? <p className="auth-error" role="alert">{error}</p> : null}

        <label>
          Usuario
          <input
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>

        <Button type="submit" disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Entrar'}
        </Button>
      </form>
    </div>
  );
}
