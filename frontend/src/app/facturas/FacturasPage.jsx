import { useEffect, useMemo, useState } from 'react';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import AnulacionModal from '../../components/facturas/AnulacionModal';
import FacturaDetalleModal from '../../components/facturas/FacturaDetalleModal';
import FacturaEstadoChip from '../../components/facturas/FacturaEstadoChip';
import Button from '../../components/ui/Button';
import Pagination from '../../components/ui/Pagination';
import { facturaService } from '../../services/facturaService';
import { usuarioService } from '../../services/usuarioService';
import { configuracionService } from '../../services/configuracionService';
import { imprimirRecibo } from '../../components/caja/FacturaTicket';
import { descargarCsv, archivoDocumento, textoCelda } from '../../utils/exportCsv';
import { dinero } from '../../utils/formato';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

function fechaCorta(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function IconoBuscar() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-3-3" />
    </svg>
  );
}

function IconoTitulo() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
      <rect x="3" y="6" width="18" height="12" rx="2" />
      <circle cx="12" cy="12" r="2.5" />
      <path d="M7 9h.01M17 15h.01" strokeLinecap="round" />
    </svg>
  );
}

function IconoFiltroLimpiar() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M4 6h16M7 12h10M9 18h6" />
      <path d="M18 4l2 2M20 4l-2 2" />
    </svg>
  );
}

function IconoOjo() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
      <circle cx="12" cy="12" r="2.5" />
    </svg>
  );
}

function IconoDoc() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
      <path d="M14 2v6h6M8 13h8M8 17h5" />
    </svg>
  );
}

function IconoAnular() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <path d="M15 9l-6 6M9 9l6 6" />
    </svg>
  );
}

export default function FacturasPage() {
  const { tieneRol } = useAuth();
  const esAdmin = tieneRol('ADMIN');

  const [facturas, setFacturas] = useState([]);
  const [cajeros, setCajeros] = useState([]);
  const [busqueda, setBusqueda] = useState('');
  const [cajeroId, setCajeroId] = useState('');
  const [estado, setEstado] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [seleccionada, setSeleccionada] = useState(null);
  const [modalDetalle, setModalDetalle] = useState(false);
  const [modalAnular, setModalAnular] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [negocio, setNegocio] = useState(null);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  async function cargar(params = {}, paginaDestino = 0) {
    setCargando(true);
    setError('');
    try {
      const respuesta = await facturaService.listar({
        estado: (params.estado ?? estado) || undefined,
        busqueda: params.busqueda ?? busqueda,
        desde: (params.desde ?? desde) || undefined,
        hasta: (params.hasta ?? hasta) || undefined,
        cajeroId: (params.cajeroId ?? cajeroId) || undefined,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setFacturas(contenidoPagina(respuesta));
      setPaginaMeta(metaPagina(respuesta));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
    configuracionService.negocio().then(setNegocio).catch(() => setNegocio(null));
    if (esAdmin) {
      usuarioService.listar({ rol: 'CAJERO', activo: true, tamano: 200 })
        .then((r) => setCajeros(contenidoPagina(r)))
        .catch(() => setCajeros([]));
    }
  }, []);

  useEffect(() => {
    if (!esAdmin && facturas.length) {
      const mapa = new Map();
      facturas.forEach((f) => {
        if (f.cajeroId) mapa.set(f.cajeroId, { id: f.cajeroId, nombreCompleto: f.cajeroNombre });
      });
      setCajeros([...mapa.values()]);
    }
  }, [esAdmin, facturas]);

  const resumen = useMemo(() => {
    const pagadas = facturas.filter((f) => f.estado === 'EMITIDA');
    const anuladas = facturas.filter((f) => f.estado === 'ANULADA');
    const total = pagadas.reduce((s, f) => s + Number(f.total || 0), 0);
    return { pagadas: pagadas.length, anuladas: anuladas.length, total };
  }, [facturas]);

  function aplicarFiltros(paginaDestino = 0) {
    cargar({}, paginaDestino);
  }

  async function verDetalle(factura) {
    setError('');
    try {
      const detalle = await facturaService.obtener(factura.id);
      setSeleccionada(detalle);
      setModalDetalle(true);
    } catch {
      setSeleccionada(factura);
      setModalDetalle(true);
    }
  }

  function limpiarFiltros() {
    setBusqueda('');
    setCajeroId('');
    setEstado('');
    setDesde('');
    setHasta('');
    cargar({ busqueda: '', cajeroId: '', estado: '', desde: '', hasta: '' }, 0);
  }

  function abrirAnulacion(factura) {
    setSeleccionada(factura);
    setModalAnular(true);
  }

  async function confirmarAnulacion(payload) {
    if (!seleccionada) return;
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const actualizada = await facturaService.anular(seleccionada.id, payload);
      setSeleccionada(actualizada);
      setModalAnular(false);
      setModalDetalle(false);
      setOk('Factura anulada. El stock regresó al inventario.');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function imprimirFe(factura) {
    await verDetalle(factura);
    setTimeout(() => imprimirRecibo(), 400);
  }

  function exportarCsv() {
    descargarCsv(archivoDocumento('Control de facturas', { desde, hasta }), [
      { key: 'numero', label: 'Número interno de factura', format: (f) => textoCelda(f.numero) },
      { key: 'numeroFiscal', label: 'Número fiscal', format: (f) => textoCelda(f.numeroFiscal, 'Sin número fiscal') },
      { key: 'fechaEmision', label: 'Fecha de emisión', format: (f) => fechaCorta(f.fechaEmision) },
      { key: 'cajeroNombre', label: 'Cajero', format: (f) => textoCelda(f.cajeroNombre) },
      { key: 'clienteNombre', label: 'Cliente', format: (f) => textoCelda(f.clienteNombre, 'Consumidor final') },
      { key: 'total', label: 'Total (C$)', format: (f) => dinero(f.total) },
      { key: 'estado', label: 'Estado', format: (f) => (f.estado === 'EMITIDA' ? 'Emitida' : f.estado === 'ANULADA' ? 'Anulada' : textoCelda(f.estado)) },
    ], facturas, {
      titulo: 'Control y auditoría de facturas',
      subtitulo: 'Historial de comprobantes emitidos y anulados',
      desde: desde || undefined,
      hasta: hasta || undefined,
      filtros: [
        estado && `Estado: ${estado === 'EMITIDA' ? 'Pagada' : estado === 'ANULADA' ? 'Anulada' : estado}`,
        cajeroId && (cajeros.find((c) => String(c.id) === String(cajeroId))?.nombreCompleto
          ? `Cajero: ${cajeros.find((c) => String(c.id) === String(cajeroId)).nombreCompleto}`
          : 'Cajero filtrado'),
        busqueda.trim() && `Búsqueda: ${busqueda.trim()}`,
      ].filter(Boolean).join(' · ') || 'Sin filtros adicionales',
    });
  }

  return (
    <section className="fca-page">
      <header className="fca-topbar">
        <div className="fca-title-block">
          <div className="fca-title-row">
            <span className="fca-title-icon" aria-hidden="true"><IconoTitulo /></span>
            <h1>Control y Auditoría de Facturas</h1>
          </div>
          <p>Historial inmutable de ventas. Inspecciona transacciones y genera anulaciones con retorno de inventario.</p>
        </div>
        <label className="fca-search">
          <IconoBuscar />
          <input
            type="search"
            placeholder="Buscar por N° factura, cajero…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') aplicarFiltros(0); }}
          />
        </label>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <div className="cat-kpi-grid">
        <article className="cat-kpi">
          <span>Pagadas</span>
          <strong>{resumen.pagadas}</strong>
          <small>En la página actual</small>
        </article>
        <article className="cat-kpi">
          <span>Anuladas</span>
          <strong>{resumen.anuladas}</strong>
          <small>En la página actual</small>
        </article>
        <article className="cat-kpi">
          <span>Total cobrado</span>
          <strong>{dinero(resumen.total)}</strong>
          <small>Facturas pagadas de esta página</small>
        </article>
      </div>

      <article className="card fca-toolbar-card">
        <div className="fca-toolbar">
          <label>
            Cajero
            <select
              value={cajeroId}
              onChange={(e) => { setCajeroId(e.target.value); cargar({ cajeroId: e.target.value }, 0); }}
            >
              <option value="">Todos los cajeros</option>
              {cajeros.map((c) => (
                <option key={c.id} value={c.id}>{c.nombreCompleto}</option>
              ))}
            </select>
          </label>
          <label>
            Estado
            <select
              value={estado}
              onChange={(e) => { setEstado(e.target.value); cargar({ estado: e.target.value }, 0); }}
            >
              <option value="">Todos los estados</option>
              <option value="EMITIDA">Pagada</option>
              <option value="ANULADA">Anulada</option>
            </select>
          </label>
          <label>
            Desde
            <input
              type="date"
              value={desde}
              onChange={(e) => { setDesde(e.target.value); cargar({ desde: e.target.value }, 0); }}
            />
          </label>
          <label>
            Hasta
            <input
              type="date"
              value={hasta}
              onChange={(e) => { setHasta(e.target.value); cargar({ hasta: e.target.value }, 0); }}
            />
          </label>
          <button type="button" className="fca-btn-limpiar" onClick={limpiarFiltros}>
            <IconoFiltroLimpiar />
            Limpiar
          </button>
        </div>
      </article>

      <article className="card fca-table-card">
        <div className="fca-table-meta">
          <span>
            {cargando ? 'Cargando…' : `${paginaMeta.totalElementos} factura(s)`}
            {!cargando && resumen.pagadas ? ` · ${dinero(resumen.total)} pagadas en página` : ''}
          </span>
          <Button variant="secondary" onClick={exportarCsv} disabled={!facturas.length}>Exportar CSV</Button>
        </div>
        {cargando ? (
          <p className="placeholder">Cargando facturas…</p>
        ) : !facturas.length ? (
          <p className="placeholder">No hay facturas con los filtros actuales.</p>
        ) : (
          <div className="fca-table-wrap">
            <table className="data-table fca-table">
              <thead>
                <tr>
                  <th>N° Factura</th>
                  <th>Fecha y hora</th>
                  <th>Cajero</th>
                  <th>Cliente</th>
                  <th>Total cobrado</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {facturas.map((factura) => (
                  <tr key={factura.id}>
                    <td>
                      <strong>{factura.numero}</strong>
                      {factura.numeroFiscal ? (
                        <small className="hint fca-numero-fiscal">{factura.numeroFiscal}</small>
                      ) : null}
                    </td>
                    <td>{fechaCorta(factura.fechaEmision)}</td>
                    <td><strong className="fca-cajero">{factura.cajeroNombre || '—'}</strong></td>
                    <td>{factura.clienteNombre || 'Consumidor final'}</td>
                    <td><strong>{dinero(factura.total)}</strong></td>
                    <td><FacturaEstadoChip estado={factura.estado} /></td>
                    <td>
                      <div className="fca-actions">
                        <button type="button" className="fca-act fca-act-detalle" onClick={() => verDetalle(factura)}>
                          <IconoOjo />
                          Detalle
                        </button>
                        <button type="button" className="fca-act fca-act-fe" onClick={() => imprimirFe(factura)} title="Factura electrónica / imprimir">
                          <IconoDoc />
                          FE
                        </button>
                        <button
                          type="button"
                          className="fca-act fca-act-anular"
                          disabled={!factura.anulable}
                          title={factura.anulable ? 'Anular factura' : factura.motivoNoAnulable}
                          onClick={() => abrirAnulacion(factura)}
                        >
                          <IconoAnular />
                          Anular
                        </button>
                        {!factura.anulable && factura.motivoNoAnulable ? (
                          <span className="fca-blocked-hint" title={factura.motivoNoAnulable}>
                            Bloqueada
                          </span>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="fca-table-footer">
          <Pagination
            pagina={pagina}
            totalPaginas={paginaMeta.totalPaginas}
            totalElementos={paginaMeta.totalElementos}
            tamano={paginaMeta.tamano}
            cargando={cargando}
            onChange={(nueva) => cargar({}, nueva)}
          />
        </div>
      </article>

      <FacturaDetalleModal
        open={modalDetalle}
        factura={seleccionada}
        negocio={negocio}
        onClose={() => setModalDetalle(false)}
        onAnular={abrirAnulacion}
        onImprimir={() => imprimirRecibo()}
      />

      <AnulacionModal
        open={modalAnular && Boolean(seleccionada?.anulable)}
        factura={seleccionada}
        guardando={guardando}
        titulo={`Anular ${seleccionada?.numero || 'factura'}`}
        onClose={() => setModalAnular(false)}
        onConfirmar={confirmarAnulacion}
      />
    </section>
  );
}
