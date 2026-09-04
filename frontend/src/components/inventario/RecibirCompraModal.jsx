import { useEffect, useMemo, useState } from 'react';
import Button from '../ui/Button';
import Icon from '../ui/Icon';
import Modal from '../ui/Modal';
import { proveedorService } from '../../services/proveedorService';
import {
  cantidadAUmm,
  costoAUmm,
  costoPorPresentacion,
  costoPonderadoPreview,
} from '../../utils/costoPresentacion';
import { dinero } from '../../utils/formato';
import { previewPoliticaTrasCompra, previewSugeridoCompra, etiquetaPolitica, POLITICA_PRECIO } from '../../utils/politicaPrecio';

const compraVacia = {
  proveedorId: '',
  documentoProveedor: '',
  observacion: '',
  actualizarCostos: true,
};

const lineaNuevaVacia = {
  productoId: '',
  presentacionId: '',
  cantidad: '1',
  costoUnitario: '',
  fechaVencimiento: '',
};

function costoCoincideCatalogo(costo, catalogo) {
  if (catalogo == null || costo === '' || costo == null) return true;
  return Math.abs(Number(costo) - Number(catalogo)) <= 0.01;
}

export default function RecibirCompraModal({
  open,
  productos,
  guardando,
  modo = 'directa',
  onClose,
  onConfirmar,
}) {
  const [proveedores, setProveedores] = useState([]);
  const [compraForm, setCompraForm] = useState(compraVacia);
  const [lineasCompra, setLineasCompra] = useState([]);
  const [lineaNueva, setLineaNueva] = useState(lineaNuevaVacia);
  const [errorLocal, setErrorLocal] = useState('');
  const [precioCatalogo, setPrecioCatalogo] = useState(null);

  useEffect(() => {
    if (open) {
      proveedorService.activos().then(setProveedores).catch(() => setProveedores([]));
    } else {
      setCompraForm(compraVacia);
      setLineasCompra([]);
      setLineaNueva(lineaNuevaVacia);
      setErrorLocal('');
      setPrecioCatalogo(null);
    }
  }, [open]);

  const productoLinea = useMemo(
    () => productos.find((item) => String(item.id) === String(lineaNueva.productoId)),
    [productos, lineaNueva.productoId],
  );

  const presentacionLinea = useMemo(
    () => productoLinea?.presentaciones?.find(
      (item) => String(item.id) === String(lineaNueva.presentacionId),
    ),
    [productoLinea, lineaNueva.presentacionId],
  );

  const factorLinea = presentacionLinea?.factorAUnidadMinima ?? 1;
  const ummLinea = cantidadAUmm(lineaNueva.cantidad, factorLinea);
  const costoUmmLinea = costoAUmm(lineaNueva.costoUnitario, factorLinea);

  const simulacionInventario = useMemo(() => {
    const map = new Map();
    for (const linea of lineasCompra) {
      const producto = productos.find((item) => item.id === linea.productoId);
      if (!producto) continue;
      const previo = map.get(linea.productoId) ?? {
        stock: Number(producto.stockActual) || 0,
        costo: Number(producto.precioCompra) || 0,
      };
      const costoUmm = Number(linea.costoUnitario) / (linea.factorAUnidadMinima ?? 1);
      const costoPonderado = costoPonderadoPreview(previo.stock, previo.costo, linea.cantidadUmm, costoUmm);
      map.set(linea.productoId, {
        stock: previo.stock + linea.cantidadUmm,
        costo: costoPonderado,
      });
    }
    return map;
  }, [lineasCompra, productos]);

  const inventarioSimulado = productoLinea
    ? simulacionInventario.get(Number(lineaNueva.productoId))
    : null;
  const stockParaPreview = inventarioSimulado?.stock ?? productoLinea?.stockActual ?? 0;
  const costoParaPreview = inventarioSimulado?.costo ?? productoLinea?.precioCompra ?? 0;

  const costoPonderadoLinea = compraForm.actualizarCostos && productoLinea && lineaNueva.costoUnitario !== ''
    ? costoPonderadoPreview(
      stockParaPreview,
      costoParaPreview,
      ummLinea,
      costoUmmLinea,
    )
    : null;
  const previewPolitica = compraForm.actualizarCostos && productoLinea && costoPonderadoLinea != null
    ? previewPoliticaTrasCompra(productoLinea, costoPonderadoLinea, costoParaPreview)
    : null;
  const previewSugerido = compraForm.actualizarCostos && productoLinea && costoPonderadoLinea != null
    ? previewSugeridoCompra(productoLinea, costoParaPreview, costoPonderadoLinea)
    : null;

  const proveedorSel = useMemo(
    () => proveedores.find((item) => String(item.id) === String(compraForm.proveedorId)),
    [proveedores, compraForm.proveedorId],
  );

  const totalCompra = lineasCompra.reduce((suma, linea) => suma + linea.cantidad * linea.costoUnitario, 0);

  const resumenPoliticas = useMemo(() => {
    if (!compraForm.actualizarCostos || !lineasCompra.length) return [];
    const vistos = new Set();
    const items = [];
    for (const linea of lineasCompra) {
      if (vistos.has(linea.productoId)) continue;
      vistos.add(linea.productoId);
      const producto = productos.find((item) => item.id === linea.productoId);
      if (!producto) continue;
      const sim = simulacionInventario.get(linea.productoId);
      const preview = previewPoliticaTrasCompra(producto, sim?.costo ?? producto.precioCompra, producto.precioCompra);
      if (!preview) continue;
      if (producto.politicaPrecio === POLITICA_PRECIO.AUTOMATICO_MARKUP) continue;
      items.push({
        productoId: linea.productoId,
        nombre: linea.productoNombre,
        politica: etiquetaPolitica(producto.politicaPrecio),
        mensaje: preview.mensaje,
        precioSugerido: preview.precioSugerido,
      });
    }
    return items;
  }, [compraForm.actualizarCostos, lineasCompra, productos, simulacionInventario]);

  async function aplicarPrecioProveedor(proveedorId, productoId, presentacionId, productoFallback) {
    const presentacion = productoFallback?.presentaciones?.find(
      (item) => String(item.id) === String(presentacionId),
    );
    const factor = presentacion?.factorAUnidadMinima ?? 1;
    if (!proveedorId || !productoId || !presentacionId) {
      return costoPorPresentacion(productoFallback?.precioCompra, factor);
    }
    try {
      const catalogo = await proveedorService.consultarPrecio(proveedorId, productoId, presentacionId);
      if (catalogo?.precioUnitario != null) {
        return catalogo.precioUnitario;
      }
    } catch {
      // Sin catálogo
    }
    return costoPorPresentacion(productoFallback?.precioCompra, factor);
  }

  async function onProductoChange(productoId) {
    const producto = productos.find((item) => String(item.id) === String(productoId));
    const presentacionId = producto?.presentaciones?.[0]?.id || '';
    const costo = await aplicarPrecioProveedor(compraForm.proveedorId, productoId, presentacionId, producto);
    setLineaNueva({
      productoId,
      presentacionId,
      cantidad: '1',
      costoUnitario: costo,
      fechaVencimiento: producto?.fechaVencimiento || '',
    });
    setPrecioCatalogo(typeof costo === 'number' ? costo : null);
  }

  async function onPresentacionChange(presentacionId) {
    const costo = await aplicarPrecioProveedor(
      compraForm.proveedorId,
      lineaNueva.productoId,
      presentacionId,
      productoLinea,
    );
    setLineaNueva((actual) => ({ ...actual, presentacionId, costoUnitario: costo }));
    setPrecioCatalogo(typeof costo === 'number' ? costo : null);
  }

  async function onProveedorChange(proveedorId) {
    const proveedor = proveedores.find((item) => String(item.id) === String(proveedorId));
    setCompraForm((actual) => ({
      ...actual,
      proveedorId,
      documentoProveedor: proveedor?.documento || actual.documentoProveedor,
    }));
    if (lineaNueva.productoId && lineaNueva.presentacionId) {
      const costo = await aplicarPrecioProveedor(
        proveedorId,
        lineaNueva.productoId,
        lineaNueva.presentacionId,
        productoLinea,
      );
      setLineaNueva((actual) => ({ ...actual, costoUnitario: costo }));
      setPrecioCatalogo(typeof costo === 'number' ? costo : null);
    }
  }

  function agregarLinea() {
    if (!compraForm.proveedorId) {
      setErrorLocal('Seleccione un proveedor registrado.');
      return;
    }
    if (!lineaNueva.productoId || !lineaNueva.presentacionId || !lineaNueva.cantidad || lineaNueva.costoUnitario === '') {
      setErrorLocal('Complete producto, presentación, cantidad y costo.');
      return;
    }
    const producto = productos.find((item) => String(item.id) === String(lineaNueva.productoId));
    const presentacion = producto?.presentaciones?.find((item) => String(item.id) === String(lineaNueva.presentacionId));
    const factor = presentacion?.factorAUnidadMinima ?? 1;
    const cantidadUmm = cantidadAUmm(lineaNueva.cantidad, factor);
    const costoUmm = costoAUmm(lineaNueva.costoUnitario, factor);
    const costoPond = compraForm.actualizarCostos
      ? costoPonderadoPreview(stockParaPreview, costoParaPreview, cantidadUmm, costoUmm)
      : null;
    setLineasCompra((actual) => [
      ...actual,
      {
        productoId: Number(lineaNueva.productoId),
        presentacionId: Number(lineaNueva.presentacionId),
        cantidad: Number(lineaNueva.cantidad),
        cantidadUmm,
        factorAUnidadMinima: factor,
        costoUnitario: Number(lineaNueva.costoUnitario),
        precioCatalogo,
        productoNombre: producto?.nombre,
        presentacionNombre: presentacion?.nombre,
        politicaPrecio: producto?.politicaPrecio,
        costoPonderadoEstimado: costoPond,
        fechaVencimiento: lineaNueva.fechaVencimiento || null,
      },
    ]);
    setLineaNueva(lineaNuevaVacia);
    setPrecioCatalogo(null);
    setErrorLocal('');
  }

  const costoDistintoCatalogo = !costoCoincideCatalogo(lineaNueva.costoUnitario, precioCatalogo);

  function usarPrecioCatalogo() {
    if (precioCatalogo == null) return;
    setLineaNueva((actual) => ({ ...actual, costoUnitario: precioCatalogo }));
    setErrorLocal('');
  }

  function submit(event) {
    event.preventDefault();
    if (!compraForm.proveedorId) {
      setErrorLocal('Seleccione un proveedor.');
      return;
    }
    if (!lineasCompra.length) {
      setErrorLocal('Agregue al menos una línea.');
      return;
    }
    onConfirmar?.({
      proveedorId: Number(compraForm.proveedorId),
      documentoProveedor: compraForm.documentoProveedor.trim() || undefined,
      observacion: compraForm.observacion.trim() || undefined,
      actualizarCostos: compraForm.actualizarCostos,
      detalles: lineasCompra.map((linea) => ({
        productoId: linea.productoId,
        presentacionId: linea.presentacionId,
        cantidad: linea.cantidad,
        costoUnitario: linea.costoUnitario,
        fechaVencimiento: linea.fechaVencimiento || undefined,
      })),
    });
  }

  const esOrden = modo === 'orden';
  const titulo = esOrden ? 'Nueva orden de compra' : 'Recepción directa';
  const subtitulo = esOrden
    ? 'Registra el pedido sin mover stock'
    : 'Orden + ingreso de stock en un paso';
  const etiquetaBoton = esOrden ? 'Crear orden' : 'Recibir compra';

  return (
    <Modal
      open={open}
      subtitle={subtitulo}
      title={titulo}
      onClose={onClose}
      size="lg"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-recibir-compra" disabled={guardando || !proveedores.length}>
            {guardando ? 'Guardando…' : etiquetaBoton}
          </Button>
        </>
      )}
    >
      <form id="form-recibir-compra" className="com-modal-form" onSubmit={submit}>
        {errorLocal ? <p className="pos-alert" role="alert">{errorLocal}</p> : null}
        {!proveedores.length ? (
          <p className="pay-note warn">
            No hay proveedores activos. Regístrelos en Proveedores antes de continuar.
          </p>
        ) : null}

        <div className="com-modal-layout">
          <section className="com-modal-section">
            <h3>Datos generales</h3>
            <div className="form-grid">
              <label>
                Proveedor
                <select
                  value={compraForm.proveedorId}
                  onChange={(e) => onProveedorChange(e.target.value)}
                  required
                >
                  <option value="">Seleccione proveedor</option>
                  {proveedores.map((proveedor) => (
                    <option key={proveedor.id} value={proveedor.id}>{proveedor.nombre}</option>
                  ))}
                </select>
              </label>
              <label>
                Documento / factura
                <input
                  value={compraForm.documentoProveedor}
                  onChange={(e) => setCompraForm((actual) => ({ ...actual, documentoProveedor: e.target.value }))}
                  placeholder={proveedorSel?.documento || 'Nº de factura del proveedor'}
                />
                <small className="field-hint">No repita un documento ya usado con este proveedor.</small>
              </label>
              <label>
                Observación
                <input
                  value={compraForm.observacion}
                  onChange={(e) => setCompraForm((actual) => ({ ...actual, observacion: e.target.value }))}
                />
              </label>
              <label className="check">
                <input
                  type="checkbox"
                  checked={compraForm.actualizarCostos}
                  onChange={(e) => setCompraForm((actual) => ({ ...actual, actualizarCostos: e.target.checked }))}
                />
                Actualizar costo ponderado y evaluar política de precio
              </label>
              {previewPolitica && previewPolitica.politica === POLITICA_PRECIO.MANUAL ? (
                <p className="field-hint com-politica-preview">{previewPolitica.mensaje}</p>
              ) : null}
            </div>

            {previewSugerido ? (
              <div className="com-sugerido-preview" role="status">
                <h4>Recomendación de precio (política sugerido)</h4>
                <p>{previewSugerido.mensaje}</p>
                <dl className="com-markup-preview-grid">
                  <div><dt>Costo anterior</dt><dd>{dinero(previewSugerido.costoAnterior)}/bot.</dd></div>
                  <div><dt>Costo ponderado</dt><dd>{dinero(previewSugerido.costoPonderadoUmm)}/bot.</dd></div>
                  <div><dt>Venta actual POS</dt><dd>{dinero(previewSugerido.ventaActual)}</dd></div>
                  <div className="com-markup-preview-nuevo">
                    <dt>Precio sugerido</dt>
                    <dd>{dinero(previewSugerido.precioSugerido)}</dd>
                  </div>
                </dl>
                <p className="field-hint">No se cambia el POS automáticamente. Quedará en historial de precios.</p>
              </div>
            ) : null}

            <h3>Agregar línea</h3>
            <p className="com-conversion-hint">
              Ingrese la cantidad en la presentación elegida. Ejemplo: <strong>20 cajas × 24</strong> ={' '}
              <strong>480 botellas</strong> en inventario.
            </p>
            <div className="form-grid com-line-form">
              <label>
                Producto
                <select
                  value={lineaNueva.productoId}
                  onChange={(e) => onProductoChange(e.target.value)}
                >
                  <option value="">Seleccione</option>
                  {productos.map((producto) => (
                    <option key={producto.id} value={producto.id}>
                      {producto.codigo} · {producto.nombre}
                    </option>
                  ))}
                </select>
                {productoLinea ? (
                  <small className="field-hint com-politica-producto">
                    Política: <strong>{etiquetaPolitica(productoLinea.politicaPrecio)}</strong>
                    {productoLinea.margenObjetivoPct != null ? ` · Margen ${productoLinea.margenObjetivoPct}%` : ''}
                  </small>
                ) : null}
              </label>
              <label>
                Presentación
                <select
                  value={lineaNueva.presentacionId}
                  onChange={(e) => onPresentacionChange(e.target.value)}
                >
                  <option value="">Seleccione</option>
                  {(productoLinea?.presentaciones || []).map((presentacion) => (
                    <option key={presentacion.id} value={presentacion.id}>
                      {presentacion.nombre} (×{presentacion.factorAUnidadMinima} bot.)
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Cantidad ({presentacionLinea?.nombre || 'presentación'})
                <input
                  type="number"
                  min="1"
                  value={lineaNueva.cantidad}
                  onChange={(e) => setLineaNueva((actual) => ({ ...actual, cantidad: e.target.value }))}
                />
                {presentacionLinea && lineaNueva.cantidad ? (
                  <small className="field-hint com-umm-hint">
                    = <strong>{ummLinea} botellas</strong> en inventario
                  </small>
                ) : null}
              </label>
              <label className={costoDistintoCatalogo ? 'field-warn' : ''}>
                Costo por {presentacionLinea?.nombre?.toLowerCase() || 'presentación'} (factura)
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={lineaNueva.costoUnitario}
                  onChange={(e) => setLineaNueva((actual) => ({ ...actual, costoUnitario: e.target.value }))}
                />
                {precioCatalogo != null ? (
                  <small className={`field-hint${costoDistintoCatalogo ? ' warn' : ''}`}>
                    Catálogo anterior: {dinero(precioCatalogo)} / {presentacionLinea?.nombre || 'unidad'}
                    {costoDistintoCatalogo ? ' · se actualizará al recibir' : ''}
                  </small>
                ) : null}
                {costoUmmLinea !== '' && lineaNueva.costoUnitario !== '' ? (
                  <small className="field-hint">
                    = {dinero(costoUmmLinea)} por botella
                  </small>
                ) : null}
                {costoPonderadoLinea != null && compraForm.actualizarCostos ? (
                  <small className="field-hint com-ponderacion-hint">
                    Costo ponderado estimado: {dinero(costoPonderadoLinea)}/bot.
                    {' '}(base {dinero(costoParaPreview)}, stock {stockParaPreview} bot.)
                  </small>
                ) : null}
                {costoDistintoCatalogo && precioCatalogo != null ? (
                  <Button type="button" variant="secondary" className="com-usar-catalogo-btn" onClick={usarPrecioCatalogo}>
                    Restaurar catálogo ({dinero(precioCatalogo)})
                  </Button>
                ) : null}
              </label>
              <label>
                Vencimiento
                <input
                  type="date"
                  value={lineaNueva.fechaVencimiento}
                  onChange={(e) => setLineaNueva((actual) => ({ ...actual, fechaVencimiento: e.target.value }))}
                />
              </label>
              <Button type="button" variant="secondary" className="com-add-line-btn" onClick={agregarLinea}>
                <Icon name="plus" size={16} strokeWidth={2.5} />
                Agregar
              </Button>
            </div>
          </section>

          <aside className="com-modal-aside">
            <header className="com-modal-aside-head">
              <h3>Líneas del pedido</h3>
              <strong>{dinero(totalCompra)}</strong>
            </header>
            {!lineasCompra.length ? (
              <p className="com-modal-empty">Agregue productos al pedido.</p>
            ) : (
              <table className="com-lines-table com-lines-table-compact">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Cant.</th>
                    <th>Botellas</th>
                    <th>Total</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {lineasCompra.map((linea, indice) => (
                    <tr key={`${linea.productoId}-${indice}`}>
                      <td>
                        {linea.productoNombre}
                        {linea.presentacionNombre ? (
                          <small>{linea.presentacionNombre} ×{linea.factorAUnidadMinima}</small>
                        ) : null}
                      </td>
                      <td>{linea.cantidad}</td>
                      <td>{linea.cantidadUmm}</td>
                      <td>{dinero(linea.cantidad * linea.costoUnitario)}</td>
                      <td>
                        <button
                          type="button"
                          className="com-line-remove"
                          aria-label="Quitar línea"
                          onClick={() => setLineasCompra((actual) => actual.filter((_, i) => i !== indice))}
                        >
                          <Icon name="trash" size={15} strokeWidth={2} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            {resumenPoliticas.length ? (
              <div className="com-politica-resumen com-sugerido-resumen">
                <h4>Avisos de precio (sugerido / manual)</h4>
                <ul>
                  {resumenPoliticas.map((item) => (
                    <li key={item.productoId}>
                      <strong>{item.nombre}</strong> ({item.politica})
                      <span>{item.mensaje}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
            {esOrden ? (
              <p className="com-modal-hint">La orden no mueve stock hasta recibirla.</p>
            ) : (
              <p className="com-modal-hint">
                Ingrese el costo real de la factura. Automático aplica venta en silencio; sugerido solo avisa.
              </p>
            )}
          </aside>
        </div>
      </form>
    </Modal>
  );
}
