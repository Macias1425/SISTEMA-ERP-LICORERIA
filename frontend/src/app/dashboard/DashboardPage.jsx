import { useAuth } from '../../auth/AuthContext';

const etiquetaRol = {
  ADMIN: 'administrador',
  CAJERO: 'cajero',
  ALMACENISTA: 'almacenista',
};

export default function DashboardPage() {
  const { usuario } = useAuth();

  return (
    <section>
      <header className="page-header">
        <h1>Dashboard</h1>
        <p>
          Sesión de {usuario?.nombreCompleto} ({etiquetaRol[usuario?.rol] || usuario?.rol}).
        </p>
      </header>
      <div className="card-grid">
        <article className="card">
          <h3>Ventas del día</h3>
          <p className="placeholder">Pendiente de conectar al API.</p>
        </article>
        <article className="card">
          <h3>Stock bajo mínimo</h3>
          <p className="placeholder">Pendiente de conectar al API.</p>
        </article>
        <article className="card">
          <h3>Facturas emitidas</h3>
          <p className="placeholder">Pendiente de conectar al API.</p>
        </article>
      </div>
    </section>
  );
}
