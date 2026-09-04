import { useEffect, useMemo, useState } from 'react';
import { costoPorPresentacion } from '../../utils/costoPresentacion';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function PrecioCatalogoModal({
  open,
  proveedor,
  productos,
  guardando,
  onClose,
  onConfirmar,
}) {
  const [productoId, setProductoId] = useState('');
  const [presentacionId, setPresentacionId] = useState('');
  const [precioUnitario, setPrecioUnitario] = useState('');

  useEffect(() => {
    if (open) {
      setProductoId('');
      setPresentacionId('');
      setPrecioUnitario('');
    }
  }, [open]);

  const productoSel = useMemo(
    () => productos.find((item) => String(item.id) === String(productoId)),
    [productos, productoId]
  );

  function submit(event) {
    event.preventDefault();
    onConfirmar?.({
      productoId: Number(productoId),
      presentacionId: Number(presentacionId),
      precioUnitario: Number(precioUnitario),
      activo: true,
    });
  }

  return (
    <Modal
      open={open}
      subtitle="Precio de compra por presentación"
      title={`Catálogo · ${proveedor?.nombre || ''}`}
      onClose={onClose}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-precio-proveedor" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Guardar precio'}
          </Button>
        </>
      )}
    >
      <form id="form-precio-proveedor" className="close-modal-form" onSubmit={submit}>
        <p className="close-modal-lead">
          Al recibir compras, el costo debe coincidir con este catálogo cuando exista precio registrado.
        </p>
        <div className="form-grid">
          <label>
            Producto
            <select
              value={productoId}
              onChange={(e) => {
                const producto = productos.find((item) => String(item.id) === e.target.value);
                const pres = producto?.presentaciones?.[0];
                setProductoId(e.target.value);
                setPresentacionId(pres?.id || '');
                setPrecioUnitario(costoPorPresentacion(producto?.precioCompra, pres?.factorAUnidadMinima));
              }}
              required
            >
              <option value="">Seleccione</option>
              {productos.filter((p) => p.activo).map((producto) => (
                <option key={producto.id} value={producto.id}>{producto.codigo} · {producto.nombre}</option>
              ))}
            </select>
          </label>
          <label>
            Presentación
            <select value={presentacionId} onChange={(e) => setPresentacionId(e.target.value)} required>
              <option value="">Seleccione</option>
              {(productoSel?.presentaciones || []).map((presentacion) => (
                <option key={presentacion.id} value={presentacion.id}>{presentacion.nombre}</option>
              ))}
            </select>
          </label>
          <label>
            Precio unitario (compra)
            <input type="number" min="0.01" step="0.01" value={precioUnitario} onChange={(e) => setPrecioUnitario(e.target.value)} required />
          </label>
        </div>
      </form>
    </Modal>
  );
}
