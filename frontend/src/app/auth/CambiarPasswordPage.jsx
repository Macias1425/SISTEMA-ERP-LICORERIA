import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import Button from '../../components/ui/Button';
import Icon from '../../components/ui/Icon';

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
    <div className="pub-shell pub-login pub-login-single">
      <div className="pub-bg" aria-hidden>
        <span className="pub-orb pub-orb-a" />
        <span className="pub-orb pub-orb-b" />
        <span className="pub-grid" />
      </div>

      <main className="pub-login-panel pub-login-panel-full">
        <form className="pub-login-card" onSubmit={onSubmit}>
          <header className="pub-login-head">
            <span className="pub-brand-mark pub-brand-mark-inline" aria-hidden>
              <Icon name="wine" size={20} strokeWidth={1.75} />
            </span>
            <h1>Cambiar contraseña</h1>
            <p>
              {debeCambiarPassword
                ? 'Su clave es temporal. Debe cambiarla para continuar.'
                : 'Ingrese la contraseña actual y la nueva.'}
            </p>
          </header>

          {error ? <p className="pub-error" role="alert">{error}</p> : null}

          <label className="pub-field">
            <span>Contraseña actual</span>
            <input
              type="password"
              autoComplete="current-password"
              value={passwordActual}
              onChange={(e) => setPasswordActual(e.target.value)}
              required
            />
          </label>
          <label className="pub-field">
            <span>Nueva contraseña</span>
            <input
              type="password"
              autoComplete="new-password"
              value={passwordNueva}
              onChange={(e) => setPasswordNueva(e.target.value)}
              required
              minLength={8}
            />
          </label>
          <label className="pub-field">
            <span>Confirmar nueva</span>
            <input
              type="password"
              autoComplete="new-password"
              value={confirmacion}
              onChange={(e) => setConfirmacion(e.target.value)}
              required
              minLength={8}
            />
          </label>
          <p className="pub-demo-hint">Mínimo 8 caracteres, con letras y números.</p>

          <Button type="submit" disabled={enviando} className="pub-login-submit">
            {enviando ? 'Guardando…' : 'Guardar contraseña'}
          </Button>
          <Button
            type="button"
            variant="secondary"
            className="pub-login-secondary"
            onClick={() => { logout(); navigate('/login', { replace: true }); }}
          >
            Cerrar sesión
          </Button>
          {!debeCambiarPassword ? (
            <Link to="/dashboard" className="pub-back-link pub-back-link-center">← Volver al panel</Link>
          ) : null}
        </form>
      </main>
    </div>
  );
}
