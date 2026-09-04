import { useEffect, useMemo, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

const mermaVacia = {
  productoId: '',
  presentacionId: '',
  cantidad: '1',
  tipo: 'ROTURA',
  motivo: '',
};

export default function SolicitarMermaModal({
  open,
  productos,
  guardando,
  onClose,
  onConfirmar,
}) {
  const [form, setForm] = useState(mermaVacia);
  const [errorLocal, setErrorLocal] = useState('');

  useEffect(() => {
    if (!open) {
      setForm(mermaVacia);
      setErrorLocal('');
    }
  }, [open]);

  const productoSel = useMemo(
    () => productos.find((item) => String(item.id) === String(form.productoId)),
    [productos, form.productoId]
  );

  function submit(event) {
    event.preventDefault();
    if ((form.motivo || '').trim().length < 5) {
      setErrorLocal('El motivo debe tener al menos 5 caracteres.');
      return;
    }
    onConfirmar?.({
      productoId: Number(form.productoId),
      presentacionId: Number(form.presentacionId),
      cantidad: Number(form.cantidad),
      tipo: form.tipo,
      motivo: form.motivo.trim(),
    });
  }

  return (
    <Modal
      open={open}
      subtitle="Requiere aprobación admin"
      title="Solicitar merma"
      onClose={onClose}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-solicitar-merma" disabled={guardando}>
            {guardando ? 'Enviando…' : 'Enviar solicitud'}
          </Button>
        </>
      )}
    >
      <form id="form-solicitar-merma" className="close-modal-form" onSubmit={submit}>
        <p className="close-modal-lead">
          La salida no afecta stock hasta que un administrador apruebe la solicitud.
        </p>
        {errorLocal ? <p className="pos-alert">{errorLocal}</p> : null}
        <div className="form-grid">
          <label>
            Producto
            <select
              value={form.productoId}
              onChange={(e) => {
                const producto = productos.find((item) => String(item.id) === e.target.value);
                setForm({
                  ...form,
                  productoId: e.target.value,
                  presentacionId: producto?.presentaciones?.[0]?.id || '',
                });
              }}
              required
            >
              <option value="">Seleccione</option>
              {productos.map((producto) => (
                <option key={producto.id} value={producto.id}>{producto.codigo} · {producto.nombre}</option>
              ))}
            </select>
          </label>
          <label>
            Presentación
            <select
              name="presentacionId"
              value={form.presentacionId}
              onChange={(e) => setForm((actual) => ({ ...actual, presentacionId: e.target.value }))}
              required
            >
              <option value="">Seleccione</option>
              {(productoSel?.presentaciones || []).map((presentacion) => (
                <option key={presentacion.id} value={presentacion.id}>{presentacion.nombre}</option>
              ))}
            </select>
          </label>
          <label>
            Cantidad
            <input
              type="number"
              min="1"
              value={form.cantidad}
              onChange={(e) => setForm((actual) => ({ ...actual, cantidad: e.target.value }))}
              required
            />
          </label>
          <label>
            Tipo
            <select value={form.tipo} onChange={(e) => setForm((actual) => ({ ...actual, tipo: e.target.value }))}>
              <option value="ROTURA">Rotura</option>
              <option value="DANADA">Dañada</option>
              <option value="MUESTRA">Muestra</option>
            </select>
          </label>
          <label>
            Motivo (mín. 5 caracteres)
            <textarea
              value={form.motivo}
              onChange={(e) => setForm((actual) => ({ ...actual, motivo: e.target.value }))}
              minLength={5}
              rows={3}
              required
            />
          </label>
        </div>
      </form>
    </Modal>
  );
}
