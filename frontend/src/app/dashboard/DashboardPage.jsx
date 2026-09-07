import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import { PERMISOS } from '../../auth/permisos';
import { Icono } from '../../components/ui/Icono';
import { cajaService } from '../../services/cajaService';
import { reporteService } from '../../services/reporteService';
import { ventaService } from '../../services/ventaService';
import { dinero } from '../../utils/formato';
import { contenidoPagina } from '../../utils/paginaUtil';

function hoyIso() {
  const ahora = new Date();
  return [
    ahora.getFullYear(),
    String(ahora.getMonth() + 1).padStart(2, '0'),
    String(ahora.getDate()).padStart(2, '0'),
  ].join('-');
}

function inicioDeMes() {
  const ahora = new Date();
  return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}-01`;
}

function claveFecha(valor) {
  const fecha = new Date(valor);
  return [
    fecha.getFullYear(),
    String(fecha.getMonth() + 1).padStart(2, '0'),
    String(fecha.getDate()).padStart(2, '0'),
  ].join('-');
}

function ultimosDias(cantidad) {
  const dias = [];
  for (let i = cantidad - 1; i >= 0; i -= 1) {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() - i);
    dias.push([
      fecha.getFullYear(),
      String(fecha.getMonth() + 1).padStart(2, '0'),
      String(fecha.getDate()).padStart(2, '0'),
    ].join('-'));
  }
  return dias;
}

const accesos = [
  { to: '/inventario', titulo: 'Inventario', detalle: 'Stock y kardex', tono: 'teal', icono: 'inventario', permisos: [PERMISOS.INVENTARIO_VER] },
  { to: '/productos', titulo: 'Productos', detalle: 'Catálogo', tono: 'orange', icono: 'productos', permisos: [PERMISOS.PRODUCTOS_VER, PERMISOS.PRODUCTOS_GESTIONAR] },
  { to: '/compras', titulo: 'Compras', detalle: 'Órdenes y recepción', tono: 'orange', icono: 'proveedores', permisos: [PERMISOS.COMPRAS_GESTIONAR] },
  { to: '/facturas', titulo: 'Facturas', detalle: 'Historial de ventas', tono: 'blue', icono: 'facturas', permisos: [PERMISOS.FACTURAS_VER] },
  { to: '/reportes', titulo: 'Reportes', detalle: 'Cortes y resúmenes', tono: 'purple', icono: 'reportes', permisos: [PERMISOS.REPORTES_VER] },
  { to: '/corte-dia', titulo: 'Corte del día', detalle: 'PDF y WhatsApp', tono: 'purple', icono: 'reportes', permisos: [PERMISOS.REPORTES_VER] },
  { to: '/finanzas', titulo: 'Finanzas', detalle: 'Margen y flujo', tono: 'teal', icono: 'finanzas', permisos: [PERMISOS.FINANZAS_VER] },
  { to: '/usuarios', titulo: 'Usuarios', detalle: 'Cuentas y roles', tono: 'blue', icono: 'usuarios', permisos: [PERMISOS.USUARIOS_VER, PERMISOS.USUARIOS_GESTIONAR] },
  { to: '/configuracion', titulo: 'Configuración', detalle: 'Parámetros del sistema', tono: 'purple', icono: 'config', permisos: [PERMISOS.CONFIG_GESTIONAR] },
  { to: '/auditoria', titulo: 'Auditoría', detalle: 'Bitácora operacional', tono: 'blue', icono: 'auditoria', permisos: [PERMISOS.AUDITORIA_VER] },
  { to: '/pos', titulo: 'POS', detalle: 'Cobrar y emitir ticket', tono: 'green', icono: 'pos', permisos: [PERMISOS.VENTAS_CREAR] },
];

const coloresPago = {
  EFECTIVO: '#2f8f5b',
  TARJETA: '#3b82f6',
  STRIPE: '#635bff',
  TRANSFERENCIA: '#7c5cbf',
  MIXTO: '#0f766e',
  CREDITO: '#d97706',
};

const etiquetaPago = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  STRIPE: 'Stripe',
  TRANSFERENCIA: 'Transferencia',
  MIXTO: 'Mixto',
  CREDITO: 'Crédito',
};

export default function DashboardPage() {
  const { usuario, tienePermiso, tieneRol } = useAuth();
  const esCajero = tieneRol('CAJERO');
  const puedeInventario = tienePermiso(PERMISOS.INVENTARIO_VER);
  const puedeCompras = tienePermiso(PERMISOS.COMPRAS_GESTIONAR);
  const puedeReportes = tienePermiso(PERMISOS.REPORTES_VER);
  const puedeVentas = tienePermiso(PERMISOS.VENTAS_CREAR);
  const [resumen, setResumen] = useState(null);
  const [mes, setMes] = useState(null);
  const [caja, setCaja] = useState(null);
  const [ventas, setVentas] = useState([]);
  const [vencimientos, setVencimientos] = useState([]);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let vivo = true;
    setCargando(true);
    Promise.all([
      reporteService.resumen().catch(() => null),
      reporteService.periodo({ desde: inicioDeMes(), hasta: hoyIso() }).catch(() => null),
      puedeReportes ? reporteService.vencimientos(30, { tamano: 200 }).catch(() => []) : Promise.resolve([]),
      puedeVentas ? cajaService.estado().catch(() => null) : Promise.resolve(null),
      puedeVentas ? ventaService.listar({ tamano: 200 }).catch(() => []) : Promise.resolve([]),
    ])
      .then(([dia, periodo, lista, estadoCaja, listaVentas]) => {
        if (!vivo) {
          return;
        }
        setResumen(dia);
        setMes(periodo);
        setVencimientos(contenidoPagina(lista));
        setCaja(estadoCaja);
        setVentas(contenidoPagina(listaVentas));
      })
      .catch((err) => {
        if (vivo) {
          setError(mensajeError(err));
        }
      })
      .finally(() => {
        if (vivo) {
          setCargando(false);
        }
      });
    return () => {
      vivo = false;
    };
  }, [puedeReportes, puedeVentas]);

  const visibles = accesos.filter((item) => tienePermiso(...item.permisos));
  const vencidos = vencimientos.filter((item) => item.estado === 'VENCIDO').length;
  const porVencer = vencimientos.filter((item) => item.estado === 'POR_VENCER').length;
  const efectivoCaja = Number(caja?.turno?.montoEsperado ?? caja?.turno?.montoInicial ?? 0);
  const resultadoMes = Number(mes?.resultado || 0);

  const flujo = useMemo(() => {
    const dias = ultimosDias(7);
    const ventasOk = ventas.filter((venta) => venta.estado === 'COMPLETADA');
    return dias.map((dia) => {
      const total = ventasOk
        .filter((venta) => claveFecha(venta.fecha) === dia)
        .reduce((suma, venta) => suma + Number(venta.total || 0), 0);
      return { dia, etiqueta: dia.slice(8), total };
    });
  }, [ventas]);

  const maxFlujo = Math.max(1, ...flujo.map((item) => item.total));

  const metodos = useMemo(() => {
    const conteo = {};
    ventas.filter((venta) => venta.estado === 'COMPLETADA').forEach((venta) => {
      const clave = venta.formaPago || 'EFECTIVO';
      conteo[clave] = (conteo[clave] || 0) + Number(venta.total || 0);
    });
    const total = Object.values(conteo).reduce((suma, valor) => suma + valor, 0);
    return Object.entries(conteo).map(([nombre, valor]) => ({
      nombre,
      valor,
      pct: total ? (valor / total) * 100 : 0,
      color: coloresPago[nombre] || '#64748b',
    }));
  }, [ventas]);

  const donut = metodos.length
    ? `conic-gradient(${metodos.reduce((acc, item, indice) => {
      const inicio = metodos.slice(0, indice).reduce((suma, actual) => suma + actual.pct, 0);
      const fin = inicio + item.pct;
      const parte = `${item.color} ${inicio}% ${fin}%`;
      return acc ? `${acc}, ${parte}` : parte;
    }, '')})`
    : 'conic-gradient(#d4ddd7 0 100%)';

  const fechaHoy = new Date().toLocaleDateString('es-NI', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  const cajaAbierta = Boolean(caja?.abierta);

  return (
    <section className="home cv-page">
      <header className="fca-topbar home-topbar">
        <div className="fca-title-block">
          <div className="fca-title-row">
            <span className="cv-title-icon home-title-icon">
              <Icono nombre="inicio" />
            </span>
            <h1>Hola, {usuario?.nombreCompleto || 'usuario'}</h1>
          </div>
          <p>
            {fechaHoy}
            {esCajero
              ? (cajaAbierta
                ? ' · Turno abierto: puede cobrar en el POS.'
                : ' · Abra caja en el POS antes de vender.')
              : ' · Panel de operación de la licorería.'}
          </p>
        </div>
        {puedeVentas ? (
          <div className="fac-header-actions">
            <Link to="/pos" className="btn">{cajaAbierta || !esCajero ? 'Ir al POS' : 'Abrir caja / POS'}</Link>
          </div>
        ) : null}
      </header>

      <div className={`mnt-status ${cajaAbierta || !puedeVentas ? 'mnt-status-ok' : 'mnt-status-advertencia'}`}>
        <strong>{puedeVentas ? (cajaAbierta ? 'Caja abierta' : 'Caja cerrada') : 'Operación'}</strong>
        <span>
          {puedeVentas
            ? (cajaAbierta
              ? `Efectivo esperado ${dinero(efectivoCaja)}`
              : 'Sin turno activo: no se pueden emitir ventas hasta abrir caja.')
            : 'Accesos según el rol autorizado por el servidor.'}
        </span>
      </div>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {cargando ? <p className="placeholder">Cargando inicio…</p> : null}

      {cargando ? null : (
      <>
      <div className="launch-grid">
        {visibles.map((item) => (
          <Link key={`${item.to}-${item.titulo}`} to={item.to} className={`launch-tile launch-${item.tono}`}>
            <span className="launch-icon">
              <Icono nombre={item.icono} />
            </span>
            <strong>{item.titulo}</strong>
            <span>{item.detalle}</span>
          </Link>
        ))}
      </div>

      <div className="metric-grid">
        {puedeVentas ? (
          <article className="metric-card metric-cyan">
            <div>
              <span className="metric-label">Efectivo en caja</span>
              <strong>{caja?.abierta ? dinero(efectivoCaja) : 'Caja cerrada'}</strong>
              <small>{caja?.abierta ? 'Turno activo' : 'Abra turno en el POS'}</small>
            </div>
            <Icono nombre="caja" className="metric-icon" />
          </article>
        ) : null}
        <article className="metric-card metric-green">
          <div>
            <span className="metric-label">Ventas del mes</span>
            <strong>{dinero(mes?.ventasTotal || 0)}</strong>
            <small>{mes?.ventasCantidad || 0} venta(s) · hoy {dinero(resumen?.ventasTotal || 0)}</small>
          </div>
          <Icono nombre="reportes" className="metric-icon" />
        </article>
        {puedeCompras ? (
          <article className="metric-card metric-purple">
            <div>
              <span className="metric-label">Compras del mes</span>
              <strong>{dinero(mes?.comprasTotal || 0)}</strong>
              <small>{mes?.comprasCantidad || 0} compra(s) recibida(s)</small>
            </div>
            <Icono nombre="inventario" className="metric-icon" />
          </article>
        ) : null}
        {puedeInventario ? (
          <article className="metric-card metric-amber">
            <div>
              <span className="metric-label">Por vencer / stock</span>
              <strong>{vencidos + porVencer}</strong>
              <small>
                {vencidos} vencido(s) · {(resumen?.alertasStock || []).length} alerta(s) de stock
              </small>
            </div>
            <Icono nombre="alerta" className="metric-icon" />
          </article>
        ) : null}
      </div>

      <div className="chart-grid">
        <article className="card chart-card">
          <div className="chart-head">
            <div>
              <h3>Movimiento de la semana</h3>
              <p className="kpi-meta">Ventas completadas de los últimos 7 días.</p>
            </div>
            <div className="chart-gain">
              <span>Resultado del mes</span>
              <strong className={resultadoMes < 0 ? 'kpi-neg' : ''}>{dinero(resultadoMes)}</strong>
            </div>
          </div>
          <div className="bar-chart">
            {flujo.map((item) => (
              <div key={item.dia} className="bar-col">
                <div className="bar-track">
                  <div className="bar-fill" style={{ height: `${(item.total / maxFlujo) * 100}%` }} />
                </div>
                <span>{item.etiqueta}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="card chart-card">
          <h3>Métodos de pago</h3>
          <p className="kpi-meta">Participación sobre ventas completadas.</p>
          <div className="donut-wrap">
            <div className="donut" style={{ background: donut }}>
              <div className="donut-hole">
                <strong>{metodos.length || 0}</strong>
                <small>método(s)</small>
              </div>
            </div>
            <ul className="donut-legend">
              {metodos.length ? metodos.map((item) => (
                <li key={item.nombre}>
                  <i style={{ background: item.color }} />
                  <span>{etiquetaPago[item.nombre] || item.nombre}</span>
                  <strong>{dinero(item.valor)}</strong>
                </li>
              )) : <li>Aún no hay ventas para graficar.</li>}
            </ul>
          </div>
        </article>
      </div>

      {(puedeInventario && (resumen?.alertasStock || []).length) ? (
        <article className="card">
          <h3>Alertas de inventario</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Código</th>
                <th>Producto</th>
                <th>Stock</th>
                <th>Alerta</th>
              </tr>
            </thead>
            <tbody>
              {resumen.alertasStock.map((alerta) => (
                <tr key={alerta.productoId || alerta.codigo}>
                  <td>{alerta.codigo}</td>
                  <td>{alerta.nombre}</td>
                  <td>{alerta.stockActual} {alerta.unidadMinima || 'UMM'}</td>
                  <td>{alerta.nivelAlerta}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </article>
      ) : (
        <article className="card home-note">
          <h3>Pendientes</h3>
          <p className="placeholder">
            {porVencer || vencidos
              ? `${vencidos + porVencer} producto(s) con vencimiento por revisar. `
              : 'No hay stock bajo ni vencimientos en atención. '}
            <Link to="/reportes?tab=vencimientos">Abrir reportes</Link>
          </p>
        </article>
      )}
      </>
      )}
    </section>
  );
}
