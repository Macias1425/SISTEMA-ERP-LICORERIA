import { useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function MermaResolucionModal({
  open,
  merma,
  modo,
  guardando,
  onClose,
  onConfirmar,
}) {
  const [adminUser, setAdminUser] = useState('');
  const [adminPass, setAdminPass] = useState('');

  function cerrar() {
    setAdminUser('');
    setAdminPass('');
    onClose?.();
  }

  function submit(event) {
    event.preventDefault();
    onConfirmar?.({
      autorizacion: { username: adminUser.trim(), password: adminPass },
    });
  }

  const aprobar = modo === 'aprobar';

  return (
    <Modal
      open={open}
      subtitle={aprobar ? 'Descuenta stock al aprobar' : 'Sin movimiento de inventario'}
      title={`${aprobar ? 'Aprobar' : 'Rechazar'} merma #${merma?.id || ''}`}
      onClose={cerrar}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={cerrar}>Cancelar</Button>
          <Button
            type="submit"
            form="form-resolver-merma"
            variant={aprobar ? 'primary' : 'danger'}
            disabled={guardando}
          >
            {guardando ? 'Procesando…' : aprobar ? 'Confirmar aprobación' : 'Confirmar rechazo'}
          </Button>
        </>
      )}
    >
      <form id="form-resolver-merma" className="close-modal-form" onSubmit={submit}>
        <p className="close-modal-lead">
          {aprobar
            ? 'Al aprobar se descuenta el stock y queda registrado en el kardex como MERMA.'
            : 'Al rechazar la solicitud queda cerrada sin afectar existencias.'}
          {' '}Requiere credenciales de administrador.
        </p>
        {merma ? (
          <p className="hint">
            {merma.cantidadPresentacion} × {merma.productoNombre} — {merma.motivo}
          </p>
        ) : null}
        <div className="pay-block">
          <label className="pay-field">
            <span>Usuario administrador</span>
            <input
              value={adminUser}
              onChange={(event) => setAdminUser(event.target.value)}
              autoComplete="username"
              required
            />
          </label>
          <label className="pay-field">
            <span>Contraseña administrador</span>
            <input
              type="password"
              value={adminPass}
              onChange={(event) => setAdminPass(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
        </div>
      </form>
    </Modal>
  );
}
