import { api, ApiError } from './api';
import { cajaService } from './cajaService';
import { compraService } from './compraService';
import { facturaService } from './facturaService';
import { inventarioService } from './inventarioService';
import { normativaService } from './normativaService';
import { productoService } from './productoService';
import { ventaService } from './ventaService';
import { contenidoPagina, listarTodos } from '../utils/paginaUtil';

function esHoy(valor) {
  if (!valor) {
    return false;
  }
  const fecha = new Date(valor);
  const hoy = new Date();
  return fecha.getFullYear() === hoy.getFullYear()
    && fecha.getMonth() === hoy.getMonth()
    && fecha.getDate() === hoy.getDate();
}

function sumar(lista, campo) {
  return lista.reduce((total, item) => total + Number(item?.[campo] || 0), 0);
}

function opcional(promesa, fallback) {
  return promesa.catch(() => fallback);
}

async function resumenDesdeApisExistentes() {
  const [ventas, facturas, compras, alertas, caja, normativa] = await Promise.all([
    opcional(listarTodos((p) => ventaService.listar(p)), []),
    opcional(listarTodos((p) => facturaService.listar(p)), []),
    opcional(listarTodos((p) => compraService.listar(p)), []),
    opcional(listarTodos((p) => inventarioService.alertas(p)), []),
    opcional(cajaService.estado(), { abierta: false }),
    opcional(normativaService.estado(), { ventaLicorPermitidaAhora: true, mensaje: '' }),
  ]);

  const ventasHoy = (Array.isArray(ventas) ? ventas : []).filter(
    (venta) => esHoy(venta.fecha) && venta.estado === 'COMPLETADA'
  );
  const facturasHoy = (Array.isArray(facturas) ? facturas : []).filter((factura) => esHoy(factura.fechaEmision));
  const comprasHoy = (Array.isArray(compras) ? compras : []).filter((compra) => esHoy(compra.fecha));
  const ventasTotal = sumar(ventasHoy, 'total');
  const comprasTotal = sumar(comprasHoy, 'total');
  const emitidas = facturasHoy.filter((factura) => factura.estado !== 'ANULADA');

  const ahora = new Date();
  const fechaLocal = [
    ahora.getFullYear(),
    String(ahora.getMonth() + 1).padStart(2, '0'),
    String(ahora.getDate()).padStart(2, '0'),
  ].join('-');

  return {
    fecha: fechaLocal,
    ventasCantidad: ventasHoy.length,
    ventasSubtotal: sumar(ventasHoy, 'subtotal'),
    ventasImpuesto: sumar(ventasHoy, 'impuesto'),
    ventasTotal,
    comprasCantidad: comprasHoy.length,
    comprasTotal,
    resultadoDia: ventasTotal - comprasTotal,
    facturasEmitidas: emitidas.length,
    facturasAnuladas: facturasHoy.length - emitidas.length,
    facturasTotalEmitido: sumar(emitidas, 'total'),
    turnosAbiertos: caja?.abierta ? 1 : 0,
    ventaLicorPermitidaAhora: Boolean(normativa?.ventaLicorPermitidaAhora),
    mensajeNormativa: normativa?.mensaje || '',
    alertasStock: Array.isArray(alertas) ? alertas : [],
    fuente: 'apis',
  };
}

function fechaLocalHoy() {
  const ahora = new Date();
  return [
    ahora.getFullYear(),
    String(ahora.getMonth() + 1).padStart(2, '0'),
    String(ahora.getDate()).padStart(2, '0'),
  ].join('-');
}

function diasEntre(desde, hasta) {
  const a = new Date(`${desde}T00:00:00`);
  const b = new Date(`${hasta}T00:00:00`);
  return Math.round((b - a) / 86400000);
}

export function clasificarVencimiento(fechaVencimiento, hoy, diasAlerta) {
  if (!fechaVencimiento) {
    return 'SIN_FECHA';
  }
  const dias = diasEntre(hoy, fechaVencimiento);
  if (dias < 0) {
    return 'VENCIDO';
  }
  if (dias <= diasAlerta) {
    return 'POR_VENCER';
  }
  return 'VIGENTE';
}

const ORDEN_VENCIMIENTO = {
  VENCIDO: 0,
  POR_VENCER: 1,
  SIN_FECHA: 2,
  VIGENTE: 3,
};

async function vencimientosDesdeProductos(diasAlerta) {
  const productos = await listarTodos((p) => productoService.listar(p));
  const hoy = fechaLocalHoy();
  return (Array.isArray(productos) ? productos : [])
    .filter((producto) => producto.activo !== false && (
      Number(producto.stockActual || 0) > 0 || producto.fechaVencimiento
    ))
    .map((producto) => {
      const fechaVencimiento = producto.fechaVencimiento || null;
      return {
        productoId: producto.id,
        codigo: producto.codigo,
        nombre: producto.nombre,
        stockActual: Number(producto.stockActual || 0),
        unidadMinima: producto.unidadMinima || 'UMM',
        fechaVencimiento,
        diasRestantes: fechaVencimiento ? diasEntre(hoy, fechaVencimiento) : null,
        estado: clasificarVencimiento(fechaVencimiento, hoy, diasAlerta),
      };
    })
    .sort((a, b) => {
      const orden = (ORDEN_VENCIMIENTO[a.estado] ?? 9) - (ORDEN_VENCIMIENTO[b.estado] ?? 9);
      if (orden !== 0) {
        return orden;
      }
      return String(a.fechaVencimiento || '9999-12-31').localeCompare(String(b.fechaVencimiento || '9999-12-31'));
    });
}

function claveFecha(valor) {
  if (!valor) {
    return '';
  }
  const fecha = new Date(valor);
  return [
    fecha.getFullYear(),
    String(fecha.getMonth() + 1).padStart(2, '0'),
    String(fecha.getDate()).padStart(2, '0'),
  ].join('-');
}

function enRango(valor, desde, hasta) {
  const clave = claveFecha(valor);
  return Boolean(clave) && clave >= desde && clave <= hasta;
}

function construirQuery(params) {
  const partes = [];
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor != null && String(valor).trim() !== '') {
      partes.push(`${encodeURIComponent(clave)}=${encodeURIComponent(valor)}`);
    }
  });
  return partes.length ? `?${partes.join('&')}` : '';
}

async function periodoDesdeApisExistentes(opciones) {
  const { desde, hasta, busqueda, formaPago, estadoFactura } = opciones;
  const [ventas, facturas, compras, productos] = await Promise.all([
    opcional(listarTodos((p) => ventaService.listar(p)), []),
    opcional(listarTodos((p) => facturaService.listar(p)), []),
    opcional(listarTodos((p) => compraService.listar(p)), []),
    opcional(listarTodos((p) => productoService.listar(p)), []),
  ]);
  const catalogo = new Map((Array.isArray(productos) ? productos : []).map((item) => [item.id, item]));
  let ventasPeriodo = (Array.isArray(ventas) ? ventas : []).filter(
    (venta) => venta.estado === 'COMPLETADA' && enRango(venta.fecha, desde, hasta)
  );
  let facturasPeriodo = (Array.isArray(facturas) ? facturas : []).filter((factura) => enRango(factura.fechaEmision, desde, hasta));
  let comprasPeriodo = (Array.isArray(compras) ? compras : []).filter((compra) => enRango(compra.fecha, desde, hasta));

  if (formaPago) {
    ventasPeriodo = ventasPeriodo.filter((venta) => venta.formaPago === formaPago);
  }
  if (estadoFactura) {
    facturasPeriodo = facturasPeriodo.filter((factura) => factura.estado === estadoFactura);
  }
  if (busqueda && busqueda.trim()) {
    const q = busqueda.trim().toLowerCase();
    ventasPeriodo = ventasPeriodo.filter((venta) =>
      (venta.numero || '').toLowerCase().includes(q)
      || (venta.clienteNombre || venta.factura?.clienteNombre || '').toLowerCase().includes(q)
      || (venta.cajeroNombre || '').toLowerCase().includes(q));
    facturasPeriodo = facturasPeriodo.filter((factura) =>
      (factura.numero || '').toLowerCase().includes(q)
      || (factura.clienteNombre || '').toLowerCase().includes(q)
      || (factura.cajeroNombre || '').toLowerCase().includes(q));
    comprasPeriodo = comprasPeriodo.filter((compra) =>
      (compra.numero || '').toLowerCase().includes(q)
      || (compra.proveedorNombre || '').toLowerCase().includes(q));
  }

  const emitidas = facturasPeriodo.filter((factura) => factura.estado !== 'ANULADA');
  const ventasTotal = sumar(ventasPeriodo, 'total');
  const ventasEfectivo = sumar(ventasPeriodo.filter((v) => v.formaPago === 'EFECTIVO'), 'total');
  const ventasTarjeta = sumar(ventasPeriodo.filter((v) => v.formaPago === 'TARJETA'), 'total');
  const comprasTotal = sumar(comprasPeriodo, 'total');

  const porProducto = new Map();
  ventasPeriodo.forEach((venta) => {
    const vistos = new Set();
    (venta.detalles || []).forEach((detalle) => {
      const id = detalle.productoId;
      const producto = catalogo.get(id);
      const actual = porProducto.get(id) || {
        productoId: id,
        codigo: producto?.codigo || String(id),
        nombre: detalle.productoNombre || producto?.nombre || `Producto ${id}`,
        cantidadUmm: 0,
        tickets: 0,
        total: 0,
      };
      actual.cantidadUmm += Number(detalle.cantidadUmm || 0);
      actual.total += Number(detalle.subtotal || 0);
      if (!vistos.has(id)) {
        actual.tickets += 1;
        vistos.add(id);
      }
      porProducto.set(id, actual);
    });
  });

  let productosLista = [...porProducto.values()].sort((a, b) => b.total - a.total);
  if (busqueda && busqueda.trim()) {
    const q = busqueda.trim().toLowerCase();
    productosLista = productosLista.filter((item) =>
      (item.codigo || '').toLowerCase().includes(q)
      || (item.nombre || '').toLowerCase().includes(q));
  }

  return {
    desde,
    hasta,
    ventasCantidad: ventasPeriodo.length,
    ventasSubtotal: sumar(ventasPeriodo, 'subtotal'),
    ventasImpuesto: sumar(ventasPeriodo, 'impuesto'),
    ventasTotal,
    ventasEfectivoTotal: ventasEfectivo,
    ventasTarjetaTotal: ventasTarjeta,
    ticketPromedio: ventasPeriodo.length ? ventasTotal / ventasPeriodo.length : 0,
    comprasCantidad: comprasPeriodo.length,
    comprasTotal,
    resultado: ventasTotal - comprasTotal,
    facturasEmitidas: emitidas.length,
    facturasAnuladas: facturasPeriodo.length - emitidas.length,
    facturasTotalEmitido: sumar(emitidas, 'total'),
    turnosCantidad: 0,
    ventas: ventasPeriodo.map((venta) => ({
      ventaId: venta.id,
      numero: venta.numero,
      fecha: venta.fecha,
      clienteNombre: venta.clienteNombre || venta.factura?.clienteNombre || 'Consumidor final',
      cajeroNombre: venta.cajeroNombre,
      formaPago: venta.formaPago,
      tipoCliente: venta.tipoClienteAplicado,
      subtotal: venta.subtotal,
      impuesto: venta.impuesto,
      total: venta.total,
      estado: venta.estado,
    })),
    compras: comprasPeriodo.map((compra) => ({
      compraId: compra.id,
      proveedorId: compra.proveedorId,
      numero: compra.numero,
      fecha: compra.fecha,
      proveedorNombre: compra.proveedorNombre,
      total: compra.total,
    })),
    facturas: facturasPeriodo.map((factura) => ({
      facturaId: factura.id,
      ventaId: factura.ventaId,
      numero: factura.numero,
      fechaEmision: factura.fechaEmision,
      clienteNombre: factura.clienteNombre,
      cajeroNombre: factura.cajeroNombre,
      total: factura.total,
      estado: factura.estado,
    })),
    productos: productosLista,
    turnos: [],
    fuente: 'apis',
  };
}

function recortarLista(lista, pagina = 0, tamano = 20) {
  const items = Array.isArray(lista) ? lista : [];
  const size = Number(tamano) > 0 ? Number(tamano) : 20;
  const page = Math.max(0, Number(pagina) || 0);
  const total = items.length;
  const inicio = page * size;
  return {
    contenido: items.slice(inicio, inicio + size),
    pagina: page,
    tamano: size,
    totalElementos: total,
    totalPaginas: Math.max(1, Math.ceil(total / size) || 1),
  };
}

function recortarReporte(reporte, tabla, pagina = 0, tamano = 20) {
  const clave = String(tabla || '').toLowerCase();
  if (!clave || clave === 'finanzas') {
    return {
      ...reporte,
      pagina: 0,
      tamano,
      totalElementos: 0,
      totalPaginas: 1,
    };
  }
  const campo = clave === 'caja' ? 'turnos' : clave;
  const recorte = recortarLista(reporte[campo], pagina, tamano);
  return {
    ...reporte,
    ventas: campo === 'ventas' ? recorte.contenido : [],
    compras: campo === 'compras' ? recorte.contenido : [],
    facturas: campo === 'facturas' ? recorte.contenido : [],
    productos: campo === 'productos' ? recorte.contenido : [],
    turnos: campo === 'turnos' ? recorte.contenido : [],
    pagina: recorte.pagina,
    tamano: recorte.tamano,
    totalElementos: recorte.totalElementos,
    totalPaginas: recorte.totalPaginas,
  };
}

export const reporteService = {
  async resumen() {
    try {
      const data = await api.get('/reportes/resumen');
      return { ...data, fuente: 'resumen' };
    } catch (error) {
      if (error instanceof ApiError && (error.status === 404 || error.codigo === 'RUTA_NO_ENCONTRADA')) {
        return resumenDesdeApisExistentes();
      }
      throw error;
    }
  },

  async periodo(opciones = {}) {
    const inicio = opciones.desde || fechaLocalHoy();
    const fin = opciones.hasta || fechaLocalHoy();
    const query = construirQuery({
      desde: inicio,
      hasta: fin,
      busqueda: opciones.busqueda,
      formaPago: opciones.formaPago,
      estadoFactura: opciones.estadoFactura,
      tabla: opciones.tabla,
      pagina: opciones.pagina,
      tamano: opciones.tamano,
    });
    try {
      return await api.get(`/reportes/periodo${query}`);
    } catch (error) {
      if (error instanceof ApiError && (error.status === 404 || error.codigo === 'RUTA_NO_ENCONTRADA')) {
        const completo = await periodoDesdeApisExistentes({
          desde: inicio,
          hasta: fin,
          busqueda: opciones.busqueda,
          formaPago: opciones.formaPago,
          estadoFactura: opciones.estadoFactura,
        });
        return recortarReporte(completo, opciones.tabla, opciones.pagina, opciones.tamano);
      }
      throw error;
    }
  },

  async vencimientos(dias = 30, params = {}) {
    try {
      return await api.get(`/reportes/vencimientos${construirQuery({ dias, ...params })}`);
    } catch (error) {
      if (error instanceof ApiError && (error.status === 404 || error.codigo === 'RUTA_NO_ENCONTRADA')) {
        return vencimientosDesdeProductos(dias);
      }
      throw error;
    }
  },
};
