import { useEffect, useMemo, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

const formVacio = {
  productoId: '',
  presentacionId: '',
  tipo: 'ENTRADA',
  cantidad: '1',
  incremento: true,
  motivo: '',
};

export default function MovimientoInventarioModal({
  open,
  productos,
  productoPreseleccionado,
  guardando,
  onClose,
  onConfirmar,
}) {
  const [form, setForm] = useState(formVacio);
  const [errorLocal, setErrorLocal] = useState('');

  useEffect(() => {
    if (open) {
      setForm({
        ...formVacio,
        productoId: productoPreseleccionado?.id ? String(productoPreseleccionado.id) : '',
        presentacionId: productoPreseleccionado?.presentaciones?.[0]?.id
          ? String(productoPreseleccionado.presentaciones[0].id)
          : '',
      });
      setErrorLocal('');
    }
  }, [open, productoPreseleccionado]);

  const productoSel = useMemo(
    () => productos.find((p) => String(p.id) === String(form.productoId)),
    [productos, form.productoId]
  );

  const ummEstimado = useMemo(() => {
    if (!productoSel || !form.presentacionId || !form.cantidad) return 0;
    const pres = productoSel.presentaciones?.find((p) => String(p.id) === String(form.presentacionId));
    return Number(form.cantidad) * (pres?.factorAUnidadMinima || 1);
  }, [productoSel, form.presentacionId, form.cantidad]);

  function submit(event) {
    event.preventDefault();
    if (form.tipo === 'AJUSTE' && !(form.motivo || '').trim()) {
      setErrorLocal('El ajuste requiere motivo.');
      return;
    }
    onConfirmar?.({
      productoId: Number(form.productoId),
      presentacionId: Number(form.presentacionId),
      tipo: form.tipo,
      cantidad: Number(form.cantidad),
      incremento: form.tipo === 'AJUSTE' ? Boolean(form.incremento) : true,
      motivo: form.motivo.trim() || undefined,
    });
  }

  return (
    <Modal
      open={open}
      title={form.tipo === 'ENTRADA' ? 'Entrada de stock' : 'Ajuste de inventario'}
      subtitle="Solo entradas y ajustes manuales; compras y ventas van por su módulo"
      onClose={onClose}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-mov-inv" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Registrar movimiento'}
          </Button>
        </>
      )}
    >
      <form id="form-mov-inv" className="close-modal-form" onSubmit={submit}>
        {errorLocal ? <p className="pos-alert" role="alert">{errorLocal}</p> : null}
        <div className="form-grid">
          <label>
            Producto
            <select
              value={form.productoId}
              required
              onChange={(e) => {
                const p = productos.find((item) => String(item.id) === e.target.value);
                setForm({
                  ...form,
                  productoId: e.target.value,
                  presentacionId: p?.presentaciones?.[0]?.id ? String(p.presentaciones[0].id) : '',
                });
              }}
            >
              <option value="">Seleccione</option>
              {productos.filter((p) => p.activo !== false).map((p) => (
                <option key={p.id} value={p.id}>{p.codigo} · {p.nombre} (stock {p.stockActual})</option>
              ))}
            </select>
          </label>
          <label>
            Presentación
            <select
              value={form.presentacionId}
              required
              onChange={(e) => setForm((f) => ({ ...f, presentacionId: e.target.value }))}
            >
              <option value="">Seleccione</option>
              {(productoSel?.presentaciones || []).map((p) => (
                <option key={p.id} value={p.id}>{p.nombre} (×{p.factorAUnidadMinima} UMM)</option>
              ))}
            </select>
          </label>
          <label>
            Tipo
            <select value={form.tipo} onChange={(e) => setForm((f) => ({ ...f, tipo: e.target.value }))}>
              <option value="ENTRADA">Entrada manual</option>
              <option value="AJUSTE">Ajuste (+/−)</option>
            </select>
          </label>
          <label>
            Cantidad (presentación)
            <input
              type="number"
              min="1"
              required
              value={form.cantidad}
              onChange={(e) => setForm((f) => ({ ...f, cantidad: e.target.value }))}
            />
          </label>
          {form.tipo === 'AJUSTE' ? (
            <label className="check">
              <input
                type="checkbox"
                checked={form.incremento}
                onChange={(e) => setForm((f) => ({ ...f, incremento: e.target.checked }))}
              />
              Sumar al stock (desmarque para restar)
            </label>
          ) : null}
          <label>
            Motivo {form.tipo === 'AJUSTE' ? '(obligatorio)' : '(opcional)'}
            <textarea
              rows={2}
              value={form.motivo}
              required={form.tipo === 'AJUSTE'}
              onChange={(e) => setForm((f) => ({ ...f, motivo: e.target.value }))}
            />
          </label>
        </div>
        {ummEstimado > 0 ? (
          <p className="pay-note">Impacto estimado: <strong>{ummEstimado} UMM</strong></p>
        ) : null}
      </form>
    </Modal>
  );
}
