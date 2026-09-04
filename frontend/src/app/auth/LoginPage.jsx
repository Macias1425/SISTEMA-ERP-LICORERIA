import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CAPACIDADES_ROL } from '../../auth/rolesInfo';
import { clearToken } from '../../auth/session';
import Button from '../../components/ui/Button';
import Icon from '../../components/ui/Icon';
import { DEMO_USUARIOS } from './demoUsuarios';
import { useAuthLogin } from './useAuthLogin';

export default function LoginPage() {
  const { ingresar, error, setError, enviando } = useAuthLogin();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [demoActivo, setDemoActivo] = useState('');
  const [mostrarPass, setMostrarPass] = useState(false);

  const demoSeleccionado = DEMO_USUARIOS.find((u) => u.username === demoActivo);

  useEffect(() => {
    clearToken();
  }, []);

  async function onSubmit(event) {
    event.preventDefault();
    await ingresar({ username, password });
  }

  function seleccionarDemo(usuario) {
    setUsername(usuario.username);
    setPassword(usuario.password);
    setDemoActivo(usuario.username);
    setError('');
  }

  async function entrarComoDemo(usuario) {
    setDemoActivo(usuario.username);
    setUsername(usuario.username);
    setPassword(usuario.password);
    await ingresar(usuario);
  }

  return (
    <div className="pub-shell pub-login">
      <div className="pub-bg" aria-hidden>
        <span className="pub-orb pub-orb-a" />
        <span className="pub-orb pub-orb-b" />
        <span className="pub-grid" />
      </div>

      <aside className="pub-login-showcase">
        <Link to="/" className="pub-brand pub-brand-light">
          <span className="pub-brand-mark" aria-hidden>
            <Icon name="wine" size={22} strokeWidth={1.75} />
          </span>
          <span>
            <small>Sistema de gestión</small>
            <strong>POS Licorería</strong>
          </span>
        </Link>

        <div className="pub-login-quote">
          <p className="pub-eyebrow">Permisos del servidor</p>
          <blockquote>
            {demoSeleccionado
              ? CAPACIDADES_ROL[demoSeleccionado.rolCodigo]?.resumen
              : 'Cada rol accede solo a los módulos que el backend autoriza.'}
          </blockquote>
        </div>

        {demoSeleccionado ? (
          <ul className="pub-login-perks">
            {(CAPACIDADES_ROL[demoSeleccionado.rolCodigo]?.items || []).map((item) => (
              <li key={item}><Icon name="check" size={16} /> {item}</li>
            ))}
          </ul>
        ) : (
          <ul className="pub-login-perks">
            <li><Icon name="check" size={16} /> Cajero → abre en punto de venta</li>
            <li><Icon name="check" size={16} /> Almacenista → inventario y compras</li>
            <li><Icon name="check" size={16} /> Admin → acceso completo</li>
          </ul>
        )}

        <Link to="/" className="pub-back-link">
          ← Volver al inicio
        </Link>
      </aside>

      <main className="pub-login-panel">
        <form className="pub-login-card" onSubmit={onSubmit}>
          <header className="pub-login-head">
            <h1>Iniciar sesión</h1>
            <p>Credenciales demo o usuario propio.</p>
          </header>

          {error ? <p className="pub-error" role="alert">{error}</p> : null}

          <div className="pub-demo-section">
            <p className="pub-demo-label">Cuentas demo</p>
            <div className="pub-demo-cards">
              {DEMO_USUARIOS.map((usuario) => (
                <button
                  key={usuario.username}
                  type="button"
                  className={`pub-demo-tile pub-demo-${usuario.tono}${demoActivo === usuario.username ? ' is-active' : ''}`}
                  disabled={enviando}
                  onClick={() => seleccionarDemo(usuario)}
                  onDoubleClick={() => entrarComoDemo(usuario)}
                  style={{ '--demo-accent': usuario.accent }}
                  title={`${usuario.descripcion}. Doble clic para entrar.`}
                >
                  <span className="pub-demo-tile-icon">
                    <Icon name={usuario.icon} size={20} strokeWidth={2} />
                  </span>
                  <span className="pub-demo-tile-rol">{usuario.rol}</span>
                  <span className="pub-demo-tile-user">{usuario.username}</span>
                  <span className="pub-demo-tile-go">Entrar →</span>
                </button>
              ))}
            </div>
            <p className="pub-demo-hint">Doble clic entra directo · cajero va al POS</p>
          </div>

          <div className="pub-divider"><span>o manual</span></div>

          <label className="pub-field">
            <span>Usuario</span>
            <input
              autoComplete="username"
              placeholder="cajero"
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                setDemoActivo('');
              }}
              required
            />
          </label>

          <label className="pub-field">
            <span>Contraseña</span>
            <div className="pub-pass-wrap">
              <input
                type={mostrarPass ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setDemoActivo('');
                }}
                required
              />
              <button
                type="button"
                className="pub-pass-toggle"
                onClick={() => setMostrarPass((v) => !v)}
                aria-label={mostrarPass ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                <Icon name="eye" size={18} />
              </button>
            </div>
          </label>

          <Button type="submit" disabled={enviando} className="pub-login-submit">
            {enviando ? 'Ingresando…' : 'Entrar'}
          </Button>
        </form>
      </main>
    </div>
  );
}
