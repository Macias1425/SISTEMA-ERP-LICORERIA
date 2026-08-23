import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import Button from '../../components/ui/Button';

export default function CambiarPasswordPage() {
  const { cambiarPassword, logout, debeCambiarPassword } = useAuth();
  const navigate = useNavigate();
  const [passwordActual, setPasswordActual] = useState('');
  const [passwordNueva, setPasswordNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function onSubmit(event) {
    event.preventDefault();
    setError('');
    if (passwordNueva !== confirmacion) {
      setError('La confirmación no coincide con la nueva contraseña');
      return;
    }
    setEnviando(true);
    try {
      await cambiarPassword(passwordActual, passwordNueva, confirmacion);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={onSubmit}>
        <p className="auth-kicker">Seguridad</p>
        <h1>Cambiar contraseña</h1>
        <p className="auth-lead">
          {debeCambiarPassword
            ? 'Su clave es temporal. Debe cambiarla para continuar.'
            : 'Ingrese la contraseña actual y la nueva.'}
        </p>

        {error ? <p className="auth-error" role="alert">{error}</p> : null}

        <label>
          Contraseña actual
          <input
            type="password"
            autoComplete="current-password"
            value={passwordActual}
            onChange={(e) => setPasswordActual(e.target.value)}
            required
          />
        </label>
        <label>
          Nueva contraseña
          <input
            type="password"
            autoComplete="new-password"
            value={passwordNueva}
            onChange={(e) => setPasswordNueva(e.target.value)}
            required
            minLength={8}
          />
        </label>
        <label>
          Confirmar nueva
          <input
            type="password"
            autoComplete="new-password"
            value={confirmacion}
            onChange={(e) => setConfirmacion(e.target.value)}
            required
            minLength={8}
          />
        </label>
        <p className="auth-hint">Mínimo 8 caracteres, con letras y números. No puede ser igual al usuario.</p>

        <Button type="submit" disabled={enviando}>
          {enviando ? 'Guardando…' : 'Guardar contraseña'}
        </Button>
        <Button type="button" variant="secondary" onClick={() => { logout(); navigate('/login', { replace: true }); }}>
          Cerrar sesión
        </Button>
      </form>
    </div>
  );
}
