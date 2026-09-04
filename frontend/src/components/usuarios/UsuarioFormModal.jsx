import { useEffect, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';
import HorarioEditor from '../configuracion/HorarioEditor';
import { horariosAccesoDefault, normalizarHorariosApi } from '../../utils/horarioAcceso';

const ROLES = [
  { id: 'CAJERO', etiqueta: 'Cajero — vende en POS y maneja caja' },
  { id: 'ALMACENISTA', etiqueta: 'Almacenista — inventario, compras y catálogo' },
  { id: 'ADMIN', etiqueta: 'Administrador — acceso total al sistema' },
];

const vacio = {
  username: '',
  nombreCompleto: '',
  rol: 'CAJERO',
  password: '',
  activo: true,
  horarioAccesoHabilitado: false,
  horarios: horariosAccesoDefault(),
};

export default function UsuarioFormModal({ open, usuario, guardando, onClose, onConfirmar }) {
  const [form, setForm] = useState(vacio);

  useEffect(() => {
    if (!open) {
      setForm(vacio);
      return;
    }
    if (usuario) {
      setForm({
        username: usuario.username,
        nombreCompleto: usuario.nombreCompleto,
        rol: usuario.rol,
        password: '',
        activo: Boolean(usuario.activo),
        horarioAccesoHabilitado: Boolean(usuario.horarioAccesoHabilitado),
        horarios: normalizarHorariosApi(
          usuario.horarios?.length ? usuario.horarios : horariosAccesoDefault(),
        ),
      });
    } else {
      setForm(vacio);
    }
  }, [open, usuario]);

  function onChange(event) {
    const { name, value, type, checked } = event.target;
    setForm((actual) => ({ ...actual, [name]: type === 'checkbox' ? checked : value }));
  }

  function toggleHorarioAcceso(habilitado) {
    setForm((actual) => ({
      ...actual,
      horarioAccesoHabilitado: habilitado,
      horarios: habilitado && (!actual.horarios?.length)
        ? horariosAccesoDefault()
        : actual.horarios,
    }));
  }

  function submit(event) {
    event.preventDefault();
    if (usuario) {
      onConfirmar?.({
        nombreCompleto: form.nombreCompleto.trim(),
        rol: form.rol,
        activo: Boolean(form.activo),
        horarioAccesoHabilitado: Boolean(form.horarioAccesoHabilitado),
        horarios: form.horarioAccesoHabilitado ? form.horarios : undefined,
      });
      return;
    }
    onConfirmar?.({
      username: form.username.trim().toLowerCase(),
      nombreCompleto: form.nombreCompleto.trim(),
      rol: form.rol,
      password: form.password,
    });
  }

  const editando = Boolean(usuario);
  const rolBloqueado = editando && usuario?.puedeCambiarRol === false;
  const activoBloqueado = editando && form.activo && (usuario?.esSesionActual || usuario?.puedeDesactivar === false);
  const esAdmin = form.rol === 'ADMIN';

  return (
    <Modal
      open={open}
      subtitle="Cuentas del personal con rol, acceso y horario de sesión"
      title={editando ? `Editar ${usuario.username}` : 'Nuevo usuario'}
      onClose={onClose}
      size={editando ? 'lg' : 'md'}
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-usuario" disabled={guardando}>
            {guardando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear usuario'}
          </Button>
        </>
      )}
    >
      <form id="form-usuario" className="close-modal-form" onSubmit={submit}>
        <div className="form-grid">
          <label>
            Usuario de ingreso
            <input
              name="username"
              value={form.username}
              onChange={onChange}
              autoComplete="off"
              required
              disabled={editando}
              pattern="[a-zA-Z0-9._-]{3,80}"
              placeholder="ej. caja01"
            />
          </label>
          <label>
            Nombre completo
            <input
              name="nombreCompleto"
              value={form.nombreCompleto}
              onChange={onChange}
              required
              minLength={3}
              maxLength={120}
            />
          </label>
          <label>
            Rol
            <select name="rol" value={form.rol} onChange={onChange} disabled={rolBloqueado}>
              {ROLES.map((rol) => (
                <option key={rol.id} value={rol.id}>{rol.etiqueta}</option>
              ))}
            </select>
          </label>
          {!editando ? (
            <label>
              Contraseña temporal
              <input
                name="password"
                type="password"
                value={form.password}
                onChange={onChange}
                minLength={8}
                autoComplete="new-password"
                required
              />
            </label>
          ) : (
            <label className="check">
              <input
                name="activo"
                type="checkbox"
                checked={form.activo}
                onChange={onChange}
                disabled={activoBloqueado}
              />
              Cuenta activa
            </label>
          )}
        </div>

        {editando && !esAdmin ? (
          <section className="usr-horario-section">
            <label className="check usr-horario-toggle">
              <input
                type="checkbox"
                checked={form.horarioAccesoHabilitado}
                onChange={(e) => toggleHorarioAcceso(e.target.checked)}
              />
              Restringir horario de acceso (login y sesión)
            </label>
            {form.horarioAccesoHabilitado ? (
              <HorarioEditor
                horarios={form.horarios}
                habilitado
                columnaActivo="Acceso permitido"
                onChange={(horarios) => setForm((actual) => ({ ...actual, horarios }))}
              />
            ) : (
              <p className="hint">Sin restricción: puede iniciar sesión en cualquier momento.</p>
            )}
          </section>
        ) : null}

        {editando && esAdmin ? (
          <p className="hint">Los administradores no tienen restricción de horario de acceso.</p>
        ) : null}

        <p className="hint">
          {editando
            ? 'La contraseña se resetea desde el panel de detalle. El precio de venta no cambia con las compras; el costo se pondera en inventario.'
            : 'El usuario deberá cambiar la clave en su primer ingreso.'}
        </p>
        {rolBloqueado ? (
          <p className="hint">No puede cambiar su propio rol mientras sea el único administrador activo.</p>
        ) : null}
      </form>
    </Modal>
  );
}
