import { Link } from 'react-router-dom';
import Icon from '../../components/ui/Icon';
import { CAPACIDADES_ROL } from '../../auth/rolesInfo';
import { DEMO_USUARIOS } from './demoUsuarios';

export default function LandingPage() {
  return (
    <div className="pub-shell pub-landing">
      <div className="pub-bg" aria-hidden>
        <span className="pub-orb pub-orb-a" />
        <span className="pub-orb pub-orb-b" />
        <span className="pub-orb pub-orb-c" />
        <span className="pub-grid" />
      </div>

      <header className="pub-nav">
        <Link to="/" className="pub-brand">
          <span className="pub-brand-mark" aria-hidden>
            <Icon name="wine" size={22} strokeWidth={1.75} />
          </span>
          <span>
            <small>Sistema de gestión</small>
            <strong>POS Licorería</strong>
          </span>
        </Link>
        <nav className="pub-nav-links">
          <a href="#roles">Perfiles</a>
          <Link to="/login" className="pub-btn pub-btn-gold">Iniciar sesión</Link>
        </nav>
      </header>

      <main className="pub-landing-main">
        <section className="pub-hero">
          <div className="pub-hero-copy">
            <p className="pub-eyebrow">Examen de grado · Nicaragua</p>
            <h1>
              Punto de venta
              <em> para licorerías</em>
            </h1>
            <p className="pub-hero-lead">
              Ventas en mostrador con IVA 15% en córdobas, control de caja, facturación DGI,
              inventario por lotes y compras. Cada usuario ve solo lo que su rol permite
              — validado en el servidor y reflejado en pantalla.
            </p>
            <div className="pub-hero-actions">
              <Link to="/login" className="pub-btn pub-btn-gold pub-btn-lg">
                Iniciar sesión
              </Link>
              <a href="#roles" className="pub-btn pub-btn-ghost pub-btn-lg">
                Ver perfiles demo
              </a>
            </div>
          </div>

          <div className="pub-hero-visual" aria-hidden>
            <div className="pub-mockup">
              <div className="pub-mockup-bar">
                <span /><span /><span />
                <p>Punto de venta · Cajero</p>
              </div>
              <div className="pub-mockup-body">
                <div className="pub-mockup-search">
                  <Icon name="search" size={14} />
                  Buscar producto…
                </div>
                <div className="pub-mockup-ticket">
                  <div className="pub-mockup-row head">
                    <span>Producto</span><span>Cant.</span><span>Total</span>
                  </div>
                  <div className="pub-mockup-row"><span>Ron 750ml</span><span>1</span><span>C$ 445.00</span></div>
                  <div className="pub-mockup-row"><span>Cerveza lata</span><span>6</span><span>C$ 210.00</span></div>
                  <div className="pub-mockup-total">
                    <span>Total</span>
                    <strong>C$ 655.00</strong>
                  </div>
                </div>
                <div className="pub-mockup-pay">
                  <span className="pub-mockup-chip active">Efectivo</span>
                  <span className="pub-mockup-chip">Tarjeta</span>
                  <button type="button" className="pub-mockup-cobrar">Cobrar</button>
                </div>
              </div>
            </div>
            <div className="pub-float-card pub-float-a">
              <Icon name="check" size={16} />
              Caja abierta
            </div>
            <div className="pub-float-card pub-float-b">
              <Icon name="receipt" size={16} />
              Factura emitida
            </div>
          </div>
        </section>

        <section id="roles" className="pub-modules">
          <div className="pub-section-head">
            <p className="pub-eyebrow">Datos demo incluidos</p>
            <h2>Qué puede hacer cada perfil</h2>
            <p className="pub-section-lead">
              Use las cuentas de prueba en el login. Los permisos vienen del servidor al autenticarse.
            </p>
          </div>
          <div className="pub-roles-grid">
            {DEMO_USUARIOS.map((usuario) => {
              const info = CAPACIDADES_ROL[usuario.rolCodigo];
              return (
                <article key={usuario.username} className="pub-role-card">
                  <header>
                    <span className="pub-role-icon" style={{ '--demo-accent': usuario.accent }}>
                      <Icon name={usuario.icon} size={22} strokeWidth={1.75} />
                    </span>
                    <div>
                      <h3>{info?.titulo || usuario.rol}</h3>
                      <p>{info?.resumen}</p>
                    </div>
                  </header>
                  <ul>
                    {(info?.items || []).map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                  <p className="pub-role-cred">
                    <code>{usuario.username}</code>
                    {' / '}
                    <code>{usuario.password}</code>
                  </p>
                </article>
              );
            })}
          </div>
        </section>

        <section className="pub-cta">
          <div className="pub-cta-inner">
            <div>
              <p className="pub-eyebrow">Probar ahora</p>
              <h2>Entre como cajero, admin o almacenista</h2>
              <p>El cajero abre directamente en el punto de venta.</p>
            </div>
            <Link to="/login" className="pub-btn pub-btn-gold pub-btn-lg">
              Ir al login
            </Link>
          </div>
        </section>
      </main>

      <footer className="pub-footer">
        <p>José Manuel Macías Fúnez</p>
        <p>POS Licorería · {new Date().getFullYear()}</p>
      </footer>
    </div>
  );
}
