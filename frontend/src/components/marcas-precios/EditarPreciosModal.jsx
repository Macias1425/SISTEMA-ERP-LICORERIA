import { useEffect, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';
import { precioService } from '../../services/precioService';
import { productoService } from '../../services/productoService';
import { dinero } from '../../utils/formato';

export default function EditarPreciosModal({ producto, onCerrar, onGuardado }) {
  const [base, setBase] = useState(null);
  const [precioCompra, setPrecioCompra] = useState('');
  const [precioVenta, setPrecioVenta] = useState('');
  const [precioMayorista, setPrecioMayorista] = useState('');
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!producto) return;
    setPrecioCompra(producto.precioCompra ?? '');
    setPrecioVenta(producto.precioVenta ?? '');
    setPrecioMayorista(producto.precioMayorista ?? '');
    setError('');
    setBase(null);
    productoService.obtener(producto.id).then(setBase).catch(() => setBase(null));
    precioService.delProducto(producto.id)
      .then((listas) => {
        const mayorista = listas.find((item) => item.tipoCliente === 'MAYORISTA');
        if (mayorista?.precioUmm != null) {
          setPrecioMayorista(mayorista.precioUmm);
        }
      })
      .catch(() => {});
  }, [producto]);

  async function guardar(event) {
    event.preventDefault();
    // El PUT reemplaza el producto completo: sin la ficha original se perderían
    // categoría, stock mínimo y demás campos que este modal no edita.
    if (!base) {
      setError('Aún se está cargando la ficha del producto. Intente de nuevo en un momento.');
      return;
    }
    if (Number(precioVenta) < Number(precioCompra)) {
      setError('El precio de venta no puede ser menor al costo.');
      return;
    }
    setGuardando(true);
    setError('');
    try {
      const actualizado = await productoService.actualizar(producto.id, {
        ...base,
        precioCompra: Number(precioCompra),
        precioVenta: Number(precioVenta),
      });
      if (precioMayorista !== '' && precioMayorista != null) {
        await precioService.guardar({
          productoId: producto.id,
          tipoCliente: 'MAYORISTA',
          precioUmm: Number(precioMayorista),
          volumenMinimoUmm: 0,
        });
      }
      onGuardado?.(actualizado);
      onCerrar?.();
    } catch (err) {
      setError(err?.mensaje || 'No se pudieron guardar los precios');
    } finally {
      setGuardando(false);
    }
  }

  const margen = Number(precioVenta || 0) - Number(precioCompra || 0);
  const margenPct = Number(precioVenta || 0) > 0 ? ((margen / Number(precioVenta)) * 100).toFixed(1) : '0';

  return (
    <Modal
      open={Boolean(producto)}
      title={producto?.nombre || 'Editar precios'}
      subtitle={`${producto?.codigo || ''} · ${producto?.marca || 'Sin marca'}`}
      onClose={onCerrar}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onCerrar} disabled={guardando}>Cancelar</Button>
          <Button type="submit" form="form-precios" disabled={guardando || !base}>
            {guardando ? 'Guardando…' : 'Guardar precios'}
          </Button>
        </>
      )}
    >
      {error ? <p className="auth-error" role="alert">{error}</p> : null}
      <form id="form-precios" className="form-grid" onSubmit={guardar}>
        <label>
          Precio compra (costo)
          <input type="number" min="0" step="0.01" value={precioCompra} onChange={(e) => setPrecioCompra(e.target.value)} required />
        </label>
        <label>
          Precio venta detalle
          <input type="number" min="0" step="0.01" value={precioVenta} onChange={(e) => setPrecioVenta(e.target.value)} required />
        </label>
        <label>
          Precio mayorista (UMM)
          <input type="number" min="0" step="0.01" value={precioMayorista} onChange={(e) => setPrecioMayorista(e.target.value)} />
        </label>
      </form>
      <p className="hint">Margen estimado: {dinero(margen)} ({margenPct}%)</p>
      <p className="hint">Los cambios quedan en el historial de auditoría.</p>
    </Modal>
  );
}
