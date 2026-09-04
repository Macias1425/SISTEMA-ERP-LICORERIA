import { useEffect, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

const vacio = { nombre: '', descripcion: '', activo: true };

export default function CategoriaFormModal({ open, categoria, guardando, onClose, onConfirmar }) {
  const [form, setForm] = useState(vacio);

  useEffect(() => {
    if (!open) {
      setForm(vacio);
      return;
    }
    if (categoria) {
      setForm({
        nombre: categoria.nombre,
        descripcion: categoria.descripcion || '',
        activo: Boolean(categoria.activo),
      });
    } else {
      setForm(vacio);
    }
  }, [open, categoria]);

  function submit(event) {
    event.preventDefault();
    onConfirmar?.({ ...form, activo: Boolean(form.activo) });
  }

  return (
    <Modal
      open={open}
      subtitle="Clasificación del catálogo"
      title={categoria ? `Editar ${categoria.nombre}` : 'Nueva categoría'}
      onClose={onClose}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-categoria" disabled={guardando}>
            {guardando ? 'Guardando…' : categoria ? 'Actualizar' : 'Crear categoría'}
          </Button>
        </>
      )}
    >
      <form id="form-categoria" className="close-modal-form" onSubmit={submit}>
        <div className="form-grid">
          <label>
            Nombre
            <input
              name="nombre"
              value={form.nombre}
              onChange={(e) => setForm((a) => ({ ...a, nombre: e.target.value }))}
              required
            />
          </label>
          <label>
            Descripción
            <input
              name="descripcion"
              value={form.descripcion}
              onChange={(e) => setForm((a) => ({ ...a, descripcion: e.target.value }))}
            />
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={form.activo}
              onChange={(e) => setForm((a) => ({ ...a, activo: e.target.checked }))}
            />
            Categoría activa
          </label>
        </div>
      </form>
    </Modal>
  );
}
