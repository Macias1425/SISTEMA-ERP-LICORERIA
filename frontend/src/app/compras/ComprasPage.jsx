import { useEffect, useMemo, useState } from 'react';

import { Link } from 'react-router-dom';

import { mensajeError, useAuth } from '../../auth/AuthContext';

import { PERMISOS } from '../../auth/permisos';

import CompraAnulacionModal from '../../components/compras/CompraAnulacionModal';

import CompraDetalleModal from '../../components/compras/CompraDetalleModal';

import CompraEstadoChip from '../../components/compras/CompraEstadoChip';

import RecibirOrdenModal from '../../components/compras/RecibirOrdenModal';

import RecibirCompraModal from '../../components/inventario/RecibirCompraModal';

import Button from '../../components/ui/Button';

import EmptyState from '../../components/ui/EmptyState';

import Icon from '../../components/ui/Icon';

import Pagination from '../../components/ui/Pagination';

import { compraService } from '../../services/compraService';

import { productoService } from '../../services/productoService';

import { contenidoPagina, listarTodos, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

import { dinero } from '../../utils/formato';






function fechaCorta(valor) {

  if (!valor) return '—';

  return new Date(valor).toLocaleString('es-NI', {

    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',

  });

}



export default function ComprasPage() {

  const { tienePermiso } = useAuth();

  const puedeGestionar = tienePermiso(PERMISOS.COMPRAS_GESTIONAR);



  const [productos, setProductos] = useState([]);

  const [compras, setCompras] = useState([]);

  const [compraSel, setCompraSel] = useState(null);

  const [busqueda, setBusqueda] = useState('');

  const [estado, setEstado] = useState('');

  const [desde, setDesde] = useState('');

  const [hasta, setHasta] = useState('');

  const [modalModo, setModalModo] = useState(null);

  const [compraRecibir, setCompraRecibir] = useState(null);

  const [modalDetalle, setModalDetalle] = useState(false);

  const [modalAnular, setModalAnular] = useState(false);

  const [error, setError] = useState('');

  const [ok, setOk] = useState('');

  const [cargando, setCargando] = useState(true);

  const [guardando, setGuardando] = useState(false);

  const [pagina, setPagina] = useState(0);

  const [paginaMeta, setPaginaMeta] = useState({

    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,

  });



  async function cargarCompras(params = {}, paginaDestino = 0) {

    setCargando(true);

    setError('');

    try {

      const respuesta = await compraService.listar({

        busqueda: params.busqueda ?? busqueda,

        estado: (params.estado ?? estado) || undefined,

        desde: (params.desde ?? desde) || undefined,

        hasta: (params.hasta ?? hasta) || undefined,

        pagina: paginaDestino,

        tamano: TAMANO_PAGINA_DEFAULT,

      });

      setCompras(contenidoPagina(respuesta));

      setPaginaMeta(metaPagina(respuesta));

      setPagina(paginaDestino);

    } catch (err) {

      setError(mensajeError(err));

    } finally {

      setCargando(false);

    }

  }


  useEffect(() => {
    cargarProductos();
    cargarCompras();
  }, []);

  async function cargarProductos() {
    try {
      const lista = await listarTodos((p) => productoService.listar({ activo: true, ...p }));
      setProductos(lista);
      return lista;
    } catch {
      setProductos([]);
      return [];
    }
  }

  async function abrirModalCompra(modo) {
    await cargarProductos();
    setModalModo(modo);
  }



  const resumen = useMemo(() => {

    const pendientes = compras.filter((c) => c.estado === 'PENDIENTE' || c.estado === 'PARCIAL');

    const recibidas = compras.filter((c) => c.estado === 'RECIBIDA');

    const totalRecibido = recibidas.reduce((s, c) => s + Number(c.total || 0), 0);

    return { pendientes: pendientes.length, recibidas: recibidas.length, totalRecibido };

  }, [compras]);



  async function verCompra(compra) {

    try {

      setCompraSel(await compraService.obtener(compra.id));

    } catch {

      setCompraSel(compra);

    }

    setModalDetalle(true);

  }



  async function confirmarModal(payload) {

    setGuardando(true);

    setError('');

    setOk('');

    try {

      let resultado;

      if (modalModo === 'orden') {

        resultado = await compraService.crearOrden(payload);

        setOk(`Orden ${resultado.numero} creada. Reciba la mercancía cuando llegue.`);

      } else {

        resultado = await compraService.recibir(payload);

        setOk(`Compra ${resultado.numero} recibida. Stock y costos actualizados.`);
      }

      setModalModo(null);
      setCompraSel(resultado);
      setModalDetalle(true);
      await Promise.all([cargarCompras({}, pagina), cargarProductos()]);

    } catch (err) {

      setError(mensajeError(err));

    } finally {

      setGuardando(false);

    }

  }



  async function recibirOrden(compra) {
    try {
      const detalle = await compraService.obtener(compra.id);
      setCompraRecibir(detalle);
    } catch {
      setCompraRecibir(compra);
    }
  }

  async function confirmarRecepcionOrden(id, payload) {
    setGuardando(true);
    setError('');
    try {
      const actualizada = await compraService.recibirOrden(id, payload);
      setCompraSel(actualizada);
      setCompraRecibir(actualizada);
      const tieneSugerido = actualizada.impactosRecepcion?.some(
        (item) => item.decisionPrecio === 'SOLO_SUGERIDO',
      );
      setOk(`Recepción registrada (${actualizada.estado}).${
        tieneSugerido ? ' Revise las recomendaciones de precio sugerido.' : ''
      }`);
      await Promise.all([cargarCompras({}, pagina), cargarProductos()]);
      return actualizada;
    } catch (err) {
      setError(mensajeError(err));
      throw err;
    } finally {
      setGuardando(false);
    }
  }

  async function cerrarOrden(compra) {
    const motivo = window.prompt('Motivo del cierre (el proveedor no enviará el resto):');
    if (!motivo?.trim()) return;
    setGuardando(true);
    setError('');
    try {
      const actualizada = await compraService.cerrarOrden(compra.id, { motivo: motivo.trim() });
      setCompraSel(actualizada);
      setOk(`Orden ${actualizada.numero} cerrada como incompleta.`);
      await cargarCompras({}, pagina);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }



  async function confirmarAnulacion(payload) {

    if (!compraSel) return;

    setGuardando(true);

    setError('');

    setOk('');

    try {

      const actualizada = await compraService.anular(compraSel.id, payload);

      setCompraSel(actualizada);

      setModalAnular(false);

      setModalDetalle(true);

      setOk('Compra anulada.');

      await cargarCompras({}, pagina);

    } catch (err) {

      setError(mensajeError(err));

    } finally {

      setGuardando(false);

    }

  }



  function limpiarFiltros() {

    setBusqueda('');

    setEstado('');

    setDesde('');

    setHasta('');

    cargarCompras({ busqueda: '', estado: '', desde: '', hasta: '' }, 0);

  }



  function aplicarFiltros() {

    cargarCompras({}, 0);

  }



  return (

    <section className="com-page">

      <header className="com-topbar">

        <div className="com-title-block">

          <div className="com-title-row">

            <span className="com-title-icon" aria-hidden>

              <Icon name="truck" size={26} strokeWidth={1.75} />

            </span>

            <h1>Compras y abastecimiento</h1>

          </div>

          <p>

            Orden de compra → recepción → kardex trazable.

            {' '}

            <Link to="/proveedores">Catálogo de proveedores</Link>

          </p>

        </div>

        <label className="com-search">

          <Icon name="search" size={16} strokeWidth={2} />

          <input

            type="search"

            placeholder="Buscar número, proveedor o documento…"

            value={busqueda}

            onChange={(e) => setBusqueda(e.target.value)}

            onKeyDown={(e) => { if (e.key === 'Enter') aplicarFiltros(); }}

          />

        </label>

        {puedeGestionar ? (

          <div className="com-topbar-actions">

            <Button variant="secondary" onClick={() => abrirModalCompra('orden')}>

              <Icon name="clipboard" size={16} strokeWidth={2} />

              Nueva orden

            </Button>

            <Button onClick={() => abrirModalCompra('directa')}>

              <Icon name="check" size={16} strokeWidth={2.5} />

              Recepción directa

            </Button>

          </div>

        ) : null}

      </header>



      {error ? <p className="pos-alert" role="alert">{error}</p> : null}

      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}



      <div className="cat-kpi-grid com-kpi-grid">

        <article className="cat-kpi com-kpi-warn">

          <span>Pendientes</span>

          <strong>{resumen.pendientes}</strong>

          <small>En esta página · sin stock</small>

        </article>

        <article className="cat-kpi com-kpi-ok">

          <span>Recibidas</span>

          <strong>{resumen.recibidas}</strong>

          <small>{dinero(resumen.totalRecibido)} en página</small>

        </article>

        <article className="cat-kpi">

          <span>Registros</span>

          <strong>{paginaMeta.totalElementos}</strong>

          <small>Total con filtros</small>

        </article>

      </div>



      <article className="card com-filters-card">

        <div className="com-filters-grid">

          <label>

            Estado

            <select value={estado} onChange={(e) => setEstado(e.target.value)}>

              <option value="">Todos</option>

              <option value="PENDIENTE">Pendiente</option>

              <option value="PARCIAL">Parcial</option>

              <option value="RECIBIDA">Recibida</option>

              <option value="CERRADA">Cerrada</option>

              <option value="ANULADA">Anulada</option>

            </select>

          </label>

          <label>

            Desde

            <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />

          </label>

          <label>

            Hasta

            <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />

          </label>

        </div>

        <div className="com-filters-actions">

          <Button variant="secondary" onClick={aplicarFiltros}>Aplicar filtros</Button>

          <button type="button" className="fca-btn-limpiar" onClick={limpiarFiltros}>

            <Icon name="x" size={14} strokeWidth={2.5} />

            Limpiar

          </button>

        </div>

      </article>



      <article className="card com-table-card">

        <div className="com-table-head">

          <h3>Órdenes y recepciones</h3>

          <span className="com-table-meta">

            Página {pagina + 1} de {paginaMeta.totalPaginas}

          </span>

        </div>

        {cargando ? (

          <p className="placeholder">Cargando compras…</p>

        ) : !compras.length ? (

          <EmptyState

            icon="truck"

            title="No hay compras"

            message="Ajuste los filtros o registre una nueva orden o recepción directa."

          />

        ) : (

          <div className="fac-table-wrap">

            <table className="data-table fac-table com-table">

              <thead>

                <tr>

                  <th>Número</th>

                  <th>Proveedor</th>

                  <th>Documento</th>

                  <th>Estado</th>

                  <th>Total</th>

                  <th>Fecha</th>

                  <th className="no-print">Acciones</th>

                </tr>

              </thead>

              <tbody>

                {compras.map((compra) => (

                  <tr

                    key={compra.id}

                    className="com-row-click"

                    onClick={() => verCompra(compra)}

                  >

                    <td><strong>{compra.numero}</strong></td>

                    <td>{compra.proveedorNombre}</td>

                    <td className="com-doc">{compra.documentoProveedor || '—'}</td>

                    <td><CompraEstadoChip estado={compra.estado} /></td>

                    <td>{dinero(compra.total)}</td>

                    <td>{fechaCorta(compra.fecha)}</td>

                    <td className="no-print" onClick={(e) => e.stopPropagation()}>

                      <div className="fca-actions com-actions">

                        <button

                          type="button"

                          className="fca-act fca-act-detalle"

                          title="Ver detalle"

                          onClick={() => verCompra(compra)}

                        >

                          <Icon name="eye" size={17} strokeWidth={2} />

                        </button>

                        {puedeGestionar && (compra.estado === 'PENDIENTE' || compra.estado === 'PARCIAL') ? (

                          <button

                            type="button"

                            className="fca-act com-act-recibir"

                            title="Recibir mercancía"

                            disabled={guardando}

                            onClick={() => recibirOrden(compra)}

                          >

                            <Icon name="check" size={17} strokeWidth={2.5} />

                          </button>

                        ) : null}

                      </div>

                    </td>

                  </tr>

                ))}

              </tbody>

            </table>

          </div>

        )}

        <Pagination

          pagina={pagina}

          totalPaginas={paginaMeta.totalPaginas}

          totalElementos={paginaMeta.totalElementos}

          tamano={paginaMeta.tamano}

          cargando={cargando}

          onChange={(nueva) => cargarCompras({}, nueva)}

        />

      </article>



      <RecibirOrdenModal
        open={Boolean(compraRecibir)}
        compra={compraRecibir}
        guardando={guardando}
        onClose={() => {
          setCompraRecibir(null);
          if (compraSel) setModalDetalle(true);
        }}
        onConfirmar={confirmarRecepcionOrden}
      />

      <RecibirCompraModal

        open={Boolean(modalModo)}

        modo={modalModo || 'directa'}

        productos={productos}

        guardando={guardando}

        onClose={() => setModalModo(null)}

        onConfirmar={confirmarModal}

      />



      <CompraDetalleModal

        open={modalDetalle}

        compra={compraSel}

        puedeGestionar={puedeGestionar}

        guardando={guardando}

        onClose={() => setModalDetalle(false)}

        onRecibirOrden={recibirOrden}

        onCerrarOrden={cerrarOrden}

        onRecibirDirecta={() => {
          setModalDetalle(false);
          abrirModalCompra('directa');
        }}

        onAnular={() => setModalAnular(true)}

      />



      <CompraAnulacionModal

        open={modalAnular && Boolean(compraSel?.anulable)}

        compra={compraSel}

        guardando={guardando}

        onClose={() => setModalAnular(false)}

        onConfirmar={confirmarAnulacion}

      />

    </section>

  );

}


