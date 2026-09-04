import { dinero } from '../../utils/formato';

function fechaTicket(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function fechaCorta(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleDateString('es-NI', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function etiquetaPago(formaPago) {
  const mapa = {
    EFECTIVO: 'Efectivo',
    TARJETA: 'Tarjeta',
    TRANSFERENCIA: 'Transferencia',
    MIXTO: 'Pago mixto',
    CREDITO: 'Crédito',
  };
  return mapa[formaPago] || formaPago || '—';
}

function texto(valor) {
  const limpio = String(valor ?? '').trim();
  return limpio || '';
}

export function facturaDesdeVenta(venta) {
  if (!venta) {
    return null;
  }
  const factura = venta.factura || {};
  const lineas = factura.lineas?.length ? factura.lineas : (venta.detalles || []);
  return {
    ...factura,
    numero: factura.numero || venta.numero,
    ventaNumero: factura.ventaNumero || venta.numero,
    ventaId: factura.ventaId || venta.id,
    fechaEmision: factura.fechaEmision || venta.fecha,
    clienteNombre: factura.clienteNombre || venta.clienteNombre || 'Consumidor final',
    clienteRuc: factura.clienteRuc,
    subtotal: factura.subtotal ?? (Number(venta.total || 0) - Number(venta.impuesto || 0)),
    impuesto: factura.impuesto ?? venta.impuesto,
    total: factura.total ?? venta.total,
    formaPago: factura.formaPago || venta.formaPago,
    montoRecibido: factura.montoRecibido ?? venta.montoRecibido,
    vuelto: factura.vuelto ?? venta.vuelto,
    cajeroNombre: factura.cajeroNombre || venta.cajeroNombre,
    turnoCajaId: factura.turnoCajaId ?? venta.turnoCajaId,
    estado: factura.estado || 'EMITIDA',
    numeroFiscal: factura.numeroFiscal,
    autorizacionDgi: factura.autorizacionDgi,
    rangoAutorizado: factura.rangoAutorizado,
    fechaLimiteEmision: factura.fechaLimiteEmision,
    lineas,
  };
}

export function datosNegocio(config = {}, nombreFallback = 'Licorería POS') {
  const tasa = Number(config.tasaIva ?? 0.15);
  return {
    nombre: texto(config.nombreNegocio) || nombreFallback,
    direccion: texto(config.direccionNegocio),
    telefono: texto(config.telefonoNegocio),
    ruc: texto(config.rucEmisor),
    autorizacionDgi: texto(config.autorizacionDgi),
    tasaIva: Number.isFinite(tasa) ? tasa : 0.15,
    edadMinima: Number(config.edadMinimaAlcohol) || 18,
  };
}

export function imprimirRecibo() {
  document.body.classList.add('printing-receipt');
  const limpiar = () => {
    document.body.classList.remove('printing-receipt');
    window.removeEventListener('afterprint', limpiar);
  };
  window.addEventListener('afterprint', limpiar);
  window.setTimeout(() => window.print(), 50);
  window.setTimeout(limpiar, 2500);
}

function BarrasCodigo({ valor }) {
  const fuente = String(valor || 'TICKET');
  const anchos = [...fuente].flatMap((ch, i) => {
    const n = ch.charCodeAt(0);
    return [1 + (n % 3), 1 + ((n + i) % 2)];
  });
  return (
    <div className="recibo-barcode" aria-hidden="true">
      {anchos.map((ancho, i) => (
        <span key={i} className={i % 2 === 0 ? 'on' : 'off'} style={{ width: `${ancho}px` }} />
      ))}
    </div>
  );
}

function LineaCorte() {
  return <p className="recibo-cut" aria-hidden="true">· · · · · · · · · · · · · · · · · · · · · ·</p>;
}

export default function FacturaTicket({
  venta,
  factura: facturaProp,
  negocio: negocioProp,
  nombreNegocio = 'Licorería POS',
  onImprimir,
  compact = false,
  mostrarAcciones = true,
}) {
  const factura = facturaProp || facturaDesdeVenta(venta);
  const negocio = datosNegocio(negocioProp || { nombreNegocio }, nombreNegocio);

  if (!factura) {
    return null;
  }

  const lineas = factura.lineas || [];
  const piezas = lineas.reduce((acc, linea) => acc + Number(linea.cantidad || 0), 0);
  const tasaPct = Math.round(Number(negocio.tasaIva) * 10000) / 100;
  const numeroDoc = factura.numeroFiscal || factura.numero;
  const anulada = factura.estado === 'ANULADA';

  function imprimir() {
    if (onImprimir) {
      onImprimir(factura);
      return;
    }
    imprimirRecibo();
  }

  return (
    <article
      className={`recibo${compact ? ' recibo-compact' : ''}${anulada ? ' recibo-anulada' : ''}`}
      id="factura-ticket-print"
    >
      {anulada ? <div className="recibo-watermark">ANULADA</div> : null}

      <header className="recibo-head">
        <p className="recibo-kicker">Comprobante de venta</p>
        <h2 className="recibo-negocio">{negocio.nombre}</h2>
        {negocio.direccion ? <p>{negocio.direccion}</p> : null}
        {negocio.telefono ? <p>Tel. {negocio.telefono}</p> : null}
        {negocio.ruc ? <p>RUC {negocio.ruc}</p> : null}
      </header>

      <LineaCorte />

      <p className="recibo-doc-type">{factura.numeroFiscal ? 'Factura original' : 'Recibo de venta'}</p>
      <p className="recibo-num">{numeroDoc}</p>
      {factura.numeroFiscal ? <p className="recibo-muted">Interno {factura.numero}</p> : null}

      <dl className="recibo-meta">
        <div><dt>Fecha</dt><dd>{fechaTicket(factura.fechaEmision)}</dd></div>
        <div><dt>Cliente</dt><dd>{factura.clienteNombre || 'Consumidor final'}</dd></div>
        {factura.clienteRuc ? <div><dt>RUC cliente</dt><dd>{factura.clienteRuc}</dd></div> : null}
        {factura.cajeroNombre ? <div><dt>Cajero</dt><dd>{factura.cajeroNombre}</dd></div> : null}
        {factura.turnoCajaId ? <div><dt>Caja</dt><dd>Turno #{factura.turnoCajaId}</dd></div> : null}
        <div><dt>Pago</dt><dd>{etiquetaPago(factura.formaPago)}</dd></div>
      </dl>

      <LineaCorte />

      <table className="recibo-table">
        <thead>
          <tr>
            <th>Cant</th>
            <th>Descripción</th>
            <th>Importe</th>
          </tr>
        </thead>
        <tbody>
          {lineas.map((linea, index) => {
            const cant = Number(linea.cantidad || 0);
            const precio = Number(linea.precioUnitario ?? (cant ? Number(linea.subtotal || 0) / cant : 0));
            return (
              <tr key={`${linea.productoId}-${linea.presentacionId}-${index}`}>
                <td>{cant}</td>
                <td>
                  <strong>{linea.productoNombre}</strong>
                  {linea.presentacionNombre ? <em>{linea.presentacionNombre}</em> : null}
                  <span className="recibo-unit">{cant} × {dinero(precio)}</span>
                </td>
                <td>{dinero(linea.subtotal)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <p className="recibo-count">{piezas} artículo{piezas === 1 ? '' : 's'} · {lineas.length} línea{lineas.length === 1 ? '' : 's'}</p>

      <LineaCorte />

      <dl className="recibo-totales">
        <div><dt>Subtotal</dt><dd>{dinero(factura.subtotal)}</dd></div>
        <div><dt>IVA {tasaPct.toFixed(tasaPct % 1 ? 2 : 0)}%</dt><dd>{dinero(factura.impuesto)}</dd></div>
        <div className="recibo-total"><dt>Total</dt><dd>{dinero(factura.total)}</dd></div>
      </dl>

      {factura.formaPago === 'EFECTIVO' ? (
        <dl className="recibo-pago">
          <div><dt>Efectivo</dt><dd>{dinero(factura.montoRecibido)}</dd></div>
          <div><dt>Cambio</dt><dd>{dinero(factura.vuelto)}</dd></div>
        </dl>
      ) : (
        <p className="recibo-pago-nota">Pagado con {etiquetaPago(factura.formaPago).toLowerCase()}</p>
      )}

      {(factura.autorizacionDgi || negocio.autorizacionDgi || factura.rangoAutorizado) ? (
        <>
          <LineaCorte />
          <dl className="recibo-fiscal">
            {(factura.autorizacionDgi || negocio.autorizacionDgi) ? (
              <div><dt>Autorización DGI</dt><dd>{factura.autorizacionDgi || negocio.autorizacionDgi}</dd></div>
            ) : null}
            {factura.rangoAutorizado ? (
              <div><dt>Rango</dt><dd>{factura.rangoAutorizado}</dd></div>
            ) : null}
            {factura.fechaLimiteEmision ? (
              <div><dt>Límite emisión</dt><dd>{fechaCorta(factura.fechaLimiteEmision)}</dd></div>
            ) : null}
          </dl>
        </>
      ) : null}

      <LineaCorte />

      <BarrasCodigo valor={numeroDoc} />
      <p className="recibo-barcode-num">{numeroDoc}</p>

      <footer className="recibo-foot">
        {anulada ? (
          <p>Factura anulada. No válida como constancia de pago ni crédito fiscal.</p>
        ) : (
          <>
            <p>¡Gracias por su compra!</p>
            <small>Conserve este recibo. Montos en córdobas (C$).</small>
            <small>Venta de alcohol prohibida a menores de {negocio.edadMinima} años.</small>
          </>
        )}
        <small className="recibo-copy">Original · Cliente</small>
      </footer>

      {mostrarAcciones ? (
        <div className="recibo-actions no-print">
          <button type="button" className="btn secondary" onClick={imprimir}>
            Imprimir recibo
          </button>
        </div>
      ) : null}
    </article>
  );
}
