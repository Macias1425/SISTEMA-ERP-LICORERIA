import { useEffect, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

const vacio = {
  nombre: '',
  documento: '',
  contactoNombre: '',
  telefono: '',
  email: '',
  activo: true,
};

export default function ProveedorFormModal({ open, proveedor, guardando, onClose, onConfirmar }) {
  const [form, setForm] = useState(vacio);

  useEffect(() => {
    if (!open) {
      setForm(vacio);
      return;
    }
    if (proveedor) {
      setForm({
        nombre: proveedor.nombre,
        documento: proveedor.documento || '',
        contactoNombre: proveedor.contactoNombre || '',
        telefono: proveedor.telefono || '',
        email: proveedor.email || '',
        activo: Boolean(proveedor.activo),
      });
    } else {
      setForm(vacio);
    }
  }, [open, proveedor]);

  function submit(event) {
    event.preventDefault();
    onConfirmar?.({
      nombre: form.nombre.trim(),
      documento: form.documento.trim() || null,
      contactoNombre: form.contactoNombre.trim() || null,
      telefono: form.telefono.trim() || null,
      email: form.email.trim() || null,
      activo: Boolean(form.activo),
    });
  }

  return (
    <Modal
      open={open}
      subtitle="Define quién fija precios de compra"
      title={proveedor ? `Editar ${proveedor.nombre}` : 'Nuevo proveedor'}
      onClose={onClose}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-proveedor" disabled={guardando}>
            {guardando ? 'Guardando…' : proveedor ? 'Actualizar' : 'Crear'}
          </Button>
        </>
      )}
    >
      <form id="form-proveedor" className="close-modal-form" onSubmit={submit}>
        <div className="form-grid">
          <label>
            Nombre comercial
            <input value={form.nombre} onChange={(e) => setForm((a) => ({ ...a, nombre: e.target.value }))} required />
          </label>
          <label>
            RUC / documento
            <input value={form.documento} onChange={(e) => setForm((a) => ({ ...a, documento: e.target.value }))} />
          </label>
          <label>
            Contacto
            <input value={form.contactoNombre} onChange={(e) => setForm((a) => ({ ...a, contactoNombre: e.target.value }))} />
          </label>
          <label>
            Teléfono
            <input value={form.telefono} onChange={(e) => setForm((a) => ({ ...a, telefono: e.target.value }))} />
          </label>
          <label>
            Correo
            <input type="email" value={form.email} onChange={(e) => setForm((a) => ({ ...a, email: e.target.value }))} />
          </label>
          <label className="check">
            <input type="checkbox" checked={form.activo} onChange={(e) => setForm((a) => ({ ...a, activo: e.target.checked }))} />
            Activo para compras
          </label>
        </div>
      </form>
    </Modal>
  );
}
