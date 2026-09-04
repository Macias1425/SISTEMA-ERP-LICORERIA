import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../../services/api';
import { mensajeError } from '../../auth/AuthContext';
import Icon from '../../components/ui/Icon';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import SupervisorAuthModal from '../../components/ui/SupervisorAuthModal';
import Cart from '../../components/caja/Cart';
import EscanerBusqueda from '../../components/caja/EscanerBusqueda';
import PaymentPanel from '../../components/caja/PaymentPanel';
import FacturaTicket, { facturaDesdeVenta, imprimirRecibo } from '../../components/caja/FacturaTicket';
import ProductGrid from '../../components/caja/ProductGrid';
import { cajaService } from '../../services/cajaService';
import { clienteService } from '../../services/clienteService';
import { configuracionService } from '../../services/configuracionService';
import { facturaService } from '../../services/facturaService';
import { normativaService } from '../../services/normativaService';
import { categoriaService } from '../../services/categoriaService';
import { productoService } from '../../services/productoService';
import Pagination from '../../components/ui/Pagination';
import { contenidoPagina, listarTodos, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';
import { verificacionEdadOk } from '../../utils/edadUtil';
import { ventaService } from '../../services/ventaService';
import { useCotizacion } from '../../hooks/useCotizacion';
import { dinero } from '../../utils/formato';

function presentacionVenta(producto) {
  const activas = (producto.presentaciones || []).filter((item) => item.activo !== false);
  return activas.find((item) => item.factorAUnidadMinima === 1) || activas[0] || null;
}

function fechaHoyLocal() {
  const fecha = new Date();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

function productoVencido(producto) {
  return Boolean(producto?.fechaVencimiento && producto.fechaVencimiento < fechaHoyLocal());
}

function ummDe(linea, cantidad = linea.cantidad) {
  return cantidad * (linea.presentacion.factorAUnidadMinima || 1);
}

function nuevaClaveCobro() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `cobro-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export default function PosPage() {
  const [turno, setTurno] = useState(null);
  const [catalogo, setCatalogo] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [normativa, setNormativa] = useState(null);
  const [paginaCatalogo, setPaginaCatalogo] = useState(0);
  const [catalogoMeta, setCatalogoMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });
  const [nombreNegocio, setNombreNegocio] = useState('Licorería POS');
  const [negocio, setNegocio] = useState({ nombreNegocio: 'Licorería POS' });
  const [lineas, setLineas] = useState([]);
  const [busquedaCatalogo, setBusquedaCatalogo] = useState('');
  const [filtroCategoria, setFiltroCategoria] = useState('TODAS');
  const [clienteId, setClienteId] = useState('');
  const [tipoCliente, setTipoCliente] = useState('DETAL');
  const [formaPago, setFormaPago] = useState('EFECTIVO');
  const [confirmaEdad, setConfirmaEdad] = useState(false);
  const [fechaNacimiento, setFechaNacimiento] = useState('');
  const [montoRecibido, setMontoRecibido] = useState('');
  const [montoInicial, setMontoInicial] = useState('0');
  const [montoFisico, setMontoFisico] = useState('');
  const [observacionArqueo, setObservacionArqueo] = useState('');
  const [ticket, setTicket] = useState(null);
  const [cierre, setCierre] = useState(null);
  const [modalCierre, setModalCierre] = useState(false);
  const [modalSupervisor, setModalSupervisor] = useState(false);
  const [mensajeSupervisor, setMensajeSupervisor] = useState('');
  const [errorCierre, setErrorCierre] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [guardandoCierre, setGuardandoCierre] = useState(false);
  const [modalCatalogo, setModalCatalogo] = useState(false);
  const [ticketDisplayId, setTicketDisplayId] = useState(() => String(Date.now()).slice(-6));
  const claveCobro = useRef(nuevaClaveCobro());

  async function cargarTurno() {
    try {
      const estado = await cajaService.estado();
      const abierto = estado.abierta ? estado.turno : null;
      setTurno(abierto);
      return abierto;
    } catch (err) {
      if (!(err instanceof ApiError) || (err.status !== 404 && err.codigo !== 'NO_ENCONTRADO' && err.codigo !== 'RUTA_NO_ENCONTRADA' && err.codigo !== 'ERROR_INTERNO')) {
        throw err;
      }
      try {
        const abierto = await cajaService.abierta();
        setTurno(abierto);
        return abierto;
      } catch (abiertaErr) {
        if (abiertaErr instanceof ApiError && (abiertaErr.status === 404 || abiertaErr.codigo === 'NO_ENCONTRADO')) {
          setTurno(null);
          return null;
        }
        throw abiertaErr;
      }
    }
  }

  const buscarPos = useCallback(async (termino) => {
    const consulta = String(termino || '').trim();
    if (!consulta) return [];
    try {
      const respuesta = await productoService.pos({ busqueda: consulta, tamano: 8 });
      return contenidoPagina(respuesta);
    } catch {
      const respuesta = await productoService.listar({ activo: true, busqueda: consulta, tamano: 8 });
      return contenidoPagina(respuesta);
    }
  }, []);

  async function cargarPaginaCatalogo(paginaDestino = 0, extras = {}) {
    const categoriaNombre = extras.categoria ?? filtroCategoria;
    const categoria = categorias.find((item) => item.nombre === categoriaNombre);
    const params = {
      busqueda: (extras.busqueda ?? busquedaCatalogo).trim() || undefined,
      categoriaId: categoriaNombre === 'TODAS' ? undefined : categoria?.id,
      pagina: paginaDestino,
      tamano: TAMANO_PAGINA_DEFAULT,
    };
    try {
      const respuesta = await productoService.pos(params);
      setCatalogo(contenidoPagina(respuesta));
      setCatalogoMeta(metaPagina(respuesta));
      setPaginaCatalogo(paginaDestino);
    } catch {
      const respuesta = await productoService.listar({ activo: true, ...params });
      setCatalogo(contenidoPagina(respuesta));
      setCatalogoMeta(metaPagina(respuesta));
      setPaginaCatalogo(paginaDestino);
    }
  }

  async function cargarCatalogo() {
    const [listaClientes, listaCategorias, estado, config] = await Promise.all([
      listarTodos((p) => clienteService.listar({ activo: true, ...p })),
      listarTodos((p) => categoriaService.listar({ activo: true, ...p })).catch(() => []),
      normativaService.estado(),
      configuracionService.negocio().catch(() => null),
    ]);
    const clientesCargados = Array.isArray(listaClientes) ? listaClientes : contenidoPagina(listaClientes);
    setClientes(clientesCargados.filter((cliente) => cliente.activo !== false));
    setCategorias(Array.isArray(listaCategorias) ? listaCategorias : contenidoPagina(listaCategorias));
    setNormativa(estado);
    if (config) {
      setNegocio(config);
      if (config.nombreNegocio) {
        setNombreNegocio(config.nombreNegocio);
      }
    }
    setClienteId((actual) => {
      if (actual) {
        return actual;
      }
      const consumidor = clientesCargados.find((cliente) => cliente.nombre === 'Consumidor final');
      return String(consumidor?.id || clientesCargados[0]?.id || '');
    });
  }

  async function cargar() {
    setCargando(true);
    setError('');
    try {
      const abierto = await cargarTurno();
      await cargarCatalogo();
      if (abierto) {
        await cargarPaginaCatalogo(0);
      }
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  useEffect(() => {
    if (!modalCatalogo) return undefined;
    const id = window.setTimeout(() => {
      cargarPaginaCatalogo(0);
    }, busquedaCatalogo.trim() ? 250 : 0);
    return () => window.clearTimeout(id);
  }, [modalCatalogo, busquedaCatalogo, filtroCategoria, categorias]);

  const licorPermitido = normativa?.ventaLicorPermitidaAhora !== false;
  const hayAlcohol = lineas.some((linea) => linea.producto.esAlcoholico);
  const ummTicket = lineas.reduce(
    (suma, linea) => suma + linea.cantidad * (linea.presentacion.factorAUnidadMinima || 1),
    0
  );

  const {
    cotizacion,
    lineasCotizadas,
    cargando: cotizando,
    error: errorCotizacion,
  } = useCotizacion({ lineas, tipoCliente });

  /**
   * Mientras llega la cotización se muestra el precio de catálogo del producto para no
   * dejar el ticket en blanco; al responder el servidor manda su precio, que es el que se cobra.
   */
  const lineasConTotal = useMemo(
    () => lineas.map((linea) => {
      const factor = linea.presentacion.factorAUnidadMinima || 1;
      const cotizada = lineasCotizadas.get(`${linea.producto.id}-${linea.presentacion.id}`);
      const unitario = cotizada
        ? Number(cotizada.precioUnitario)
        : Number(linea.producto.precioVenta) * factor;
      return {
        ...linea,
        precioUnitario: unitario,
        subtotal: cotizada ? Number(cotizada.subtotal) : unitario * linea.cantidad,
        aviso: cotizada?.aviso || null,
      };
    }),
    [lineas, lineasCotizadas]
  );

  const tasaIva = Number(cotizacion?.tasaIva ?? normativa?.tasaIva ?? 0.15);
  const subtotalLocal = lineasConTotal.reduce((suma, linea) => suma + linea.subtotal, 0);
  const subtotalTicket = cotizacion ? Number(cotizacion.subtotal) : subtotalLocal;
  const impuestoTicket = cotizacion ? Number(cotizacion.impuesto) : subtotalLocal * tasaIva;
  const totalTicket = cotizacion ? Number(cotizacion.total) : subtotalLocal + impuestoTicket;
  const tipoClienteAplicado = cotizacion?.tipoClienteAplicado || tipoCliente;
  const avisosCotizacion = cotizacion?.avisos || [];
  const bloqueadoPorCotizacion = Boolean(cotizacion && cotizacion.cobrable === false);
  const maxVentasTurno = Number(normativa?.maxVentasPorTurno || 0);
  const ventasEnTurno = turno?.cantidadVentas ?? 0;
  const limiteVentasAlcanzado = maxVentasTurno > 0 && ventasEnTurno >= maxVentasTurno;
  const montoSupervisor = Number(normativa?.montoSupervisorRequerido || 0);
  const exigeSupervisorMonto = montoSupervisor > 0 && totalTicket >= montoSupervisor;
  const ventasEfectivoTurno = Number(turno?.ventasEfectivo || 0);
  const diferenciaArqueo = montoFisico === ''
    ? null
    : Math.round((Number(montoFisico) - Number(turno?.montoEsperado || 0)) * 100) / 100;
  const claseDiferencia = diferenciaArqueo === 0 ? 'ok' : diferenciaArqueo > 0 ? 'warn' : 'danger';
  const edadMinima = normativa?.edadMinimaAlcohol || 18;
  const verificacionEdad = useMemo(
    () => verificacionEdadOk(hayAlcohol, confirmaEdad, fechaNacimiento, edadMinima),
    [hayAlcohol, confirmaEdad, fechaNacimiento, edadMinima]
  );
  const reservadoUmm = useMemo(() => {
    const mapa = {};
    for (const linea of lineas) {
      mapa[linea.producto.id] = (mapa[linea.producto.id] || 0) + ummDe(linea);
    }
    return mapa;
  }, [lineas]);

  function agregar(producto, unidades = 1) {
    const presentacion = presentacionVenta(producto);
    if (!presentacion) {
      setError('El producto no tiene presentación de venta');
      return;
    }
    if (productoVencido(producto)) {
      setError(`No se puede vender "${producto.nombre}": el lote venció el ${producto.fechaVencimiento}`);
      return;
    }
    if (producto.esAlcoholico && !licorPermitido) {
      setError(normativa?.mensaje || 'No se puede vender licor en este horario');
      return;
    }
    const cantidad = Math.max(1, Number(unidades) || 1);
    const factor = presentacion.factorAUnidadMinima || 1;
    const usado = lineas
      .filter((linea) => linea.producto.id === producto.id)
      .reduce((suma, linea) => suma + ummDe(linea), 0);
    if (usado + factor * cantidad > (producto.stockActual ?? 0)) {
      setError(`Stock insuficiente para ${producto.nombre}. Disponible: ${Math.max(0, (producto.stockActual ?? 0) - usado)} UMM`);
      return;
    }
    setError('');
    setTicket(null);
    setLineas((actual) => {
      const indice = actual.findIndex((linea) => linea.producto.id === producto.id
        && linea.presentacion.id === presentacion.id);
      if (indice >= 0) {
        return actual.map((linea, i) => (
          i === indice ? { ...linea, cantidad: linea.cantidad + cantidad } : linea
        ));
      }
      return [...actual, { producto, presentacion, cantidad }];
    });
  }

  function cambiarCantidad(linea, cantidad) {
    if (cantidad < 1) {
      quitar(linea);
      return;
    }
    const factor = linea.presentacion.factorAUnidadMinima || 1;
    const usadoOtros = lineas
      .filter((item) => item.producto.id === linea.producto.id
        && item.presentacion.id !== linea.presentacion.id)
      .reduce((suma, item) => suma + ummDe(item), 0);
    const maxCantidad = Math.floor(((linea.producto.stockActual ?? 0) - usadoOtros) / factor);
    if (cantidad > maxCantidad) {
      setError(`Stock insuficiente para ${linea.producto.nombre}`);
      return;
    }
    setError('');
    setLineas((actual) => actual.map((item) => (
      item.producto.id === linea.producto.id && item.presentacion.id === linea.presentacion.id
        ? { ...item, cantidad }
        : item
    )));
  }

  function quitar(linea) {
    setLineas((actual) => actual.filter((item) => !(
      item.producto.id === linea.producto.id && item.presentacion.id === linea.presentacion.id
    )));
  }

  function cancelarVenta() {
    setLineas([]);
    setConfirmaEdad(false);
    setFechaNacimiento('');
    setMontoRecibido('');
    setError('');
    setTicket(null);
    setTicketDisplayId(String(Date.now()).slice(-6));
    claveCobro.current = nuevaClaveCobro();
  }

  function montoExacto() {
    setMontoRecibido(totalTicket.toFixed(2));
  }

  function agregarEfectivo(monto) {
    setMontoRecibido((actual) => {
      const base = actual === '' ? 0 : Number(actual);
      return String(base + monto);
    });
  }

  function onPagoChange(event) {
    const { name, value, type, checked } = event.target;
    if (name === 'clienteId') setClienteId(value);
    if (name === 'tipoCliente') setTipoCliente(value);
    if (name === 'formaPago') {
      setFormaPago(value);
      if (value !== 'EFECTIVO') {
        setMontoRecibido('');
      }
    }
    if (name === 'confirmaEdad') {
      const next = type === 'checkbox' ? checked : Boolean(value);
      setConfirmaEdad(next);
      if (next) {
        setFechaNacimiento('');
      }
      setError('');
    }
    if (name === 'fechaNacimiento') {
      setFechaNacimiento(value);
      if (value) {
        setConfirmaEdad(false);
      }
      setError('');
    }
    if (name === 'montoRecibido') setMontoRecibido(value);
  }

  function cambiarFormaPago(valor) {
    setFormaPago(valor);
    if (valor !== 'EFECTIVO') {
      setMontoRecibido('');
    }
  }

  async function abrirCaja(event) {
    event.preventDefault();
    setGuardando(true);
    setError('');
    setCierre(null);
    try {
      const abierto = await cajaService.abrir({ montoInicial: Number(montoInicial || 0) });
      setTurno(abierto);
      await cargarCatalogo();
      await cargarPaginaCatalogo(0);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function registrarVenta(autorizacionSupervisor) {
    return ventaService.registrar({
      clienteId: clienteId === '' ? null : Number(clienteId),
      tipoCliente,
      fechaNacimientoCliente: hayAlcohol && confirmaEdad ? null : (fechaNacimiento || null),
      confirmacionMayoriaEdad: hayAlcohol ? confirmaEdad : false,
      formaPago,
      montoRecibido: formaPago === 'EFECTIVO' ? Number(montoRecibido) : null,
      claveIdempotencia: claveCobro.current,
      autorizacionSupervisor: autorizacionSupervisor || undefined,
      detalles: lineas.map((linea) => ({
        productoId: linea.producto.id,
        presentacionId: linea.presentacion.id,
        cantidad: linea.cantidad,
      })),
    });
  }

  async function cobrar(autorizacionSupervisor) {
    if (!lineas.length) {
      setError('Agregue al menos un producto');
      return;
    }
    if (!verificacionEdad.ok) {
      return;
    }
    if (bloqueadoPorCotizacion) {
      setError('La cotización tiene líneas que no se pueden cobrar. Revise los avisos del ticket.');
      return;
    }
    if (formaPago === 'EFECTIVO' && (montoRecibido === '' || Number(montoRecibido) + 1e-9 < totalTicket)) {
      setError(`El efectivo recibido no cubre el total (${dinero(totalTicket)})`);
      return;
    }
    if (limiteVentasAlcanzado) {
      setError(`Límite de ventas del turno alcanzado (${maxVentasTurno}). Cierre caja o contacte al administrador.`);
      return;
    }
    if (exigeSupervisorMonto && !autorizacionSupervisor) {
      setMensajeSupervisor(
        `Esta venta (${dinero(totalTicket)}) alcanza o supera el umbral de ${dinero(montoSupervisor)}. Se requiere autorización de administrador.`
      );
      setModalSupervisor(true);
      return;
    }
    setGuardando(true);
    setError('');
    try {
      const venta = await registrarVenta(autorizacionSupervisor);
      setModalSupervisor(false);
      let ventaConFactura = venta;
      if (venta.id && !venta.factura?.lineas?.length) {
        try {
          const factura = await facturaService.porVenta(venta.id);
          ventaConFactura = { ...venta, factura };
        } catch {
          ventaConFactura = venta;
        }
      }
      setTicket(ventaConFactura);
      setLineas([]);
      setConfirmaEdad(false);
      setFechaNacimiento('');
      setMontoRecibido('');
      claveCobro.current = nuevaClaveCobro();
      setTicketDisplayId(String(Date.now()).slice(-6));
      const actualizado = await cargarTurno();
      await cargarCatalogo(actualizado);
    } catch (err) {
      if (err instanceof ApiError && err.codigo === 'AUTORIZACION_REQUERIDA') {
        setMensajeSupervisor(err.mensaje || 'Se requiere autorización de administrador para completar la venta.');
        setModalSupervisor(true);
      } else {
        setError(mensajeError(err));
      }
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarSupervisor(auth) {
    await cobrar(auth);
  }

  /** Refresca el turno antes del arqueo: el esperado debe incluir la última venta cobrada. */
  async function abrirArqueo() {
    setErrorCierre('');
    setModalCierre(true);
    try {
      const actualizado = await cargarTurno();
      if (actualizado?.montoEsperado != null) {
        setMontoFisico(String(actualizado.montoEsperado));
      } else if (actualizado?.montoInicial != null) {
        setMontoFisico(String(actualizado.montoInicial));
      } else {
        setMontoFisico('');
      }
    } catch (err) {
      setErrorCierre(mensajeError(err));
    }
  }

  async function cerrarCaja(event) {
    event.preventDefault();
    if (!turno) {
      return;
    }
    const fisico = Number(montoFisico);
    if (!Number.isFinite(fisico) || montoFisico === '') {
      setErrorCierre('Indique el efectivo contado en caja.');
      return;
    }
    setGuardandoCierre(true);
    setError('');
    setErrorCierre('');
    try {
      const cerrado = await cajaService.cerrar(turno.id, {
        montoFisico: fisico,
        observacionArqueo: observacionArqueo.trim() || null,
      });
      setCierre(cerrado);
      setTurno(null);
      setLineas([]);
      setTicket(null);
      setModalCierre(false);
      setMontoFisico('');
      setObservacionArqueo('');
      setErrorCierre('');
    } catch (err) {
      setErrorCierre(mensajeError(err));
    } finally {
      setGuardandoCierre(false);
    }
  }

  if (cargando) {
    return <p className="placeholder">Cargando caja…</p>;
  }

  if (!turno) {
    return (
      <section className="pos-open-screen">
        <article className="pos-open-card">
          <div className="pos-open-icon" aria-hidden>
            <Icon name="banknote" size={36} strokeWidth={1.75} />
          </div>
          <h1>Abrir caja</h1>
          <p className="pos-open-lead">
            Antes de vender debe abrir su turno e indicar cuánto efectivo hay en la caja registradora.
          </p>
          {error ? <p className="pos-alert" role="alert">{error}</p> : null}
          {cierre ? (
            <div className={`pos-open-summary pos-open-summary-${(cierre.resultadoArqueo || '').toLowerCase()}`}>
              <strong>Turno anterior cerrado · {cierre.resultadoArqueo}</strong>
              <span>Sistema {dinero(cierre.montoEsperado)} · Contado {dinero(cierre.montoFisico)} · Diferencia {dinero(cierre.diferencia)}</span>
            </div>
          ) : null}
          <form className="pos-open-form" onSubmit={abrirCaja}>
            <label className="pos-field pos-field-money">
              <span>Efectivo inicial en caja</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={montoInicial}
                onChange={(event) => setMontoInicial(event.target.value)}
                placeholder="0.00"
                required
              />
            </label>
            <Button type="submit" disabled={guardando}>
              {guardando ? 'Abriendo…' : 'Comenzar a vender'}
            </Button>
            <Link to="/dashboard" className="btn secondary">Volver al inicio</Link>
          </form>
        </article>
      </section>
    );
  }

  return (
    <section className="pos-page-mock">
      <header className="pos-toolbar">
        <Link to="/dashboard" className="pos-toolbar-btn">
          Inicio
        </Link>
        <EscanerBusqueda
          onBuscar={buscarPos}
          onAgregar={agregar}
          capturaGlobal={!modalCatalogo && !modalCierre && !modalSupervisor && !ticket}
        />

        <button
          type="button"
          className="pos-toolbar-btn pos-toolbar-categories"
          onClick={() => setModalCatalogo(true)}
        >
          <Icon name="grid" size={16} strokeWidth={2} />
          Categorías
        </button>

        <div className="pos-toolbar-status">
          <span>Turno #{turno.id}</span>
          <span>Ventas {ventasEnTurno}{maxVentasTurno > 0 ? `/${maxVentasTurno}` : ''}</span>
          <span>Caja {dinero(turno.montoEsperado ?? turno.montoInicial)}</span>
          <span className={licorPermitido ? 'is-ok' : 'is-warn'}>
            Licores {licorPermitido ? 'OK' : 'No'}
          </span>
        </div>

        <button type="button" className="pos-toolbar-close" onClick={abrirArqueo}>
          Cerrar caja
        </button>
      </header>

      {error && (verificacionEdad.ok || !hayAlcohol) ? (
        <p className="pos-alert" role="alert">{error}</p>
      ) : null}
      {errorCotizacion ? <p className="pos-alert" role="alert">{errorCotizacion}</p> : null}
      {limiteVentasAlcanzado ? (
        <p className="pos-alert" role="alert">Límite de ventas del turno alcanzado. Cierre caja para continuar.</p>
      ) : null}

      <div className="pos-main-mock">
        <Cart
          lineas={lineasConTotal}
          total={totalTicket}
          ticketId={ticketDisplayId}
          onCambiarCantidad={cambiarCantidad}
          onQuitar={quitar}
        />
        <PaymentPanel
          clientes={clientes}
          clienteId={clienteId}
          tipoCliente={tipoCliente}
          tipoClienteAplicado={tipoClienteAplicado}
          avisos={avisosCotizacion}
          cotizando={cotizando}
          bloqueado={bloqueadoPorCotizacion}
          formaPago={formaPago}
          hayAlcohol={hayAlcohol}
          confirmaEdad={confirmaEdad}
          fechaNacimiento={fechaNacimiento}
          edadMinima={edadMinima}
          verificacionEdad={verificacionEdad}
          licorPermitido={licorPermitido}
          ummTicket={ummTicket}
          volumenMinimo={normativa?.volumenMinimoUmm || 6}
          subtotal={subtotalTicket}
          impuesto={impuestoTicket}
          tasaIva={tasaIva}
          total={totalTicket}
          montoRecibido={montoRecibido}
          exigeSupervisor={exigeSupervisorMonto}
          limiteVentasAlcanzado={limiteVentasAlcanzado}
          cobrando={guardando}
          onChange={onPagoChange}
          onFormaPago={cambiarFormaPago}
          onCobrar={() => cobrar()}
          onCancelar={cancelarVenta}
          onMontoExacto={montoExacto}
          onAgregarEfectivo={agregarEfectivo}
        />
      </div>

      <Modal
        open={modalCatalogo}
        subtitle="Agregar productos"
        title="Catálogo"
        onClose={() => setModalCatalogo(false)}
        size="lg"
      >
        <div className="pos-catalog-modal">
          <label className="pos-search-wrap">
            <span className="pos-search-field">
              <Icon name="search" size={18} strokeWidth={2} className="pos-search-icon" />
              <input
                className="pos-search-input"
                type="search"
                placeholder="Buscar en catálogo…"
                value={busquedaCatalogo}
                onChange={(event) => setBusquedaCatalogo(event.target.value)}
              />
            </span>
          </label>
          <div className="pos-category-tabs" role="tablist" aria-label="Categorías">
            <button
              type="button"
              role="tab"
              aria-selected={filtroCategoria === 'TODAS'}
              className={`pos-category-tab${filtroCategoria === 'TODAS' ? ' active' : ''}`}
              onClick={() => setFiltroCategoria('TODAS')}
            >
              Todos
            </button>
            {categorias.map((categoria) => (
              <button
                key={categoria.id || categoria.nombre}
                type="button"
                role="tab"
                aria-selected={filtroCategoria === categoria.nombre}
                className={`pos-category-tab${filtroCategoria === categoria.nombre ? ' active' : ''}`}
                onClick={() => setFiltroCategoria(categoria.nombre)}
              >
                {categoria.nombre}
              </button>
            ))}
          </div>
          <ProductGrid
            productos={catalogo}
            licorPermitido={licorPermitido}
            reservadoUmm={reservadoUmm}
            onAgregar={(producto) => {
              agregar(producto);
              setModalCatalogo(false);
            }}
          />
          <Pagination
            pagina={paginaCatalogo}
            totalPaginas={catalogoMeta.totalPaginas}
            totalElementos={catalogoMeta.totalElementos}
            tamano={catalogoMeta.tamano}
            onChange={(nueva) => cargarPaginaCatalogo(nueva)}
          />
        </div>
      </Modal>

      <Modal
        open={Boolean(ticket)}
        subtitle="Recibo de caja"
        title={facturaDesdeVenta(ticket)?.numeroFiscal || facturaDesdeVenta(ticket)?.numero || 'Ticket de venta'}
        onClose={() => setTicket(null)}
        size="sm"
        footer={(
          <>
            <Button type="button" variant="secondary" onClick={imprimirRecibo}>
              Imprimir recibo
            </Button>
            <Button type="button" onClick={() => setTicket(null)}>Nueva venta</Button>
          </>
        )}
      >
        {ticket ? (
          <FacturaTicket
            venta={ticket}
            negocio={negocio}
            nombreNegocio={nombreNegocio}
            mostrarAcciones={false}
          />
        ) : null}
      </Modal>

      <Modal
        open={modalCierre}
        subtitle="Fin de turno"
        title="Arqueo de caja"
        onClose={() => setModalCierre(false)}
        size="md"
        footer={(
          <>
            <Button type="button" variant="secondary" onClick={() => setModalCierre(false)}>Cancelar</Button>
            <Button type="submit" form="form-cierre-caja" variant="danger" disabled={guardandoCierre || !turno}>
              {guardandoCierre ? 'Cerrando…' : 'Confirmar cierre'}
            </Button>
          </>
        )}
      >
        <form id="form-cierre-caja" className="close-modal-form" onSubmit={cerrarCaja}>
          <p className="close-modal-lead">
            Cuente el efectivo en caja y compárelo con el monto esperado del sistema.
          </p>
          {errorCierre ? <p className="pos-alert" role="alert">{errorCierre}</p> : null}
          {turno ? (
            <>
              <div className="close-modal-expected">
                <span>Esperado</span>
                <strong>{dinero(turno.montoEsperado ?? turno.montoInicial)}</strong>
              </div>
              <dl className="close-modal-desglose">
                <div><dt>Fondo de apertura</dt><dd>{dinero(turno.montoInicial)}</dd></div>
                <div><dt>Ventas en efectivo</dt><dd>{dinero(ventasEfectivoTurno)}</dd></div>
              </dl>
              {diferenciaArqueo !== null ? (
                <p className={`close-modal-diferencia ${claseDiferencia}`}>
                  {diferenciaArqueo === 0
                    ? 'Cuadra exacto con el sistema.'
                    : `${diferenciaArqueo > 0 ? 'Sobrante' : 'Faltante'} de ${dinero(Math.abs(diferenciaArqueo))}.`}
                </p>
              ) : null}
              <div className="pay-block">
                <label className="pay-field pay-field-lg">
                  <span>Efectivo contado</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={montoFisico}
                    onChange={(event) => setMontoFisico(event.target.value)}
                    required
                  />
                </label>
                <label className="pay-field">
                  <span>Observación (opcional)</span>
                  <input
                    value={observacionArqueo}
                    onChange={(event) => setObservacionArqueo(event.target.value)}
                    placeholder="Diferencia, billetes rotos, etc."
                  />
                </label>
              </div>
            </>
          ) : (
            <p className="placeholder">No hay turno abierto.</p>
          )}
        </form>
      </Modal>

      <SupervisorAuthModal
        open={modalSupervisor}
        mensaje={mensajeSupervisor}
        guardando={guardando}
        onClose={() => setModalSupervisor(false)}
        onConfirmar={confirmarSupervisor}
      />
    </section>
  );
}
