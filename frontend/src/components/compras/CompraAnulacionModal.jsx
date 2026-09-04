import { useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function CompraAnulacionModal({ open, compra, guardando, onClose, onConfirmar }) {
  const [motivo, setMotivo] = useState('');
  const [adminUser, setAdminUser] = useState('');
  const [adminPass, setAdminPass] = useState('');

  function cerrar() {
    setMotivo('');
    setAdminUser('');
    setAdminPass('');
    onClose?.();
  }

  function submit(event) {
    event.preventDefault();
    const payload = { motivo: motivo.trim() };
    if (compra?.estado === 'RECIBIDA') {
      payload.autorizacion = { username: adminUser.trim(), password: adminPass };
    }
    onConfirmar?.(payload);
  }

  const requiereAdmin = compra?.estado === 'RECIBIDA';

  return (
    <Modal
      open={open}
      subtitle={requiereAdmin ? 'Reversión de stock' : 'Cancelación de orden'}
      title={`Anular ${compra?.numero || 'compra'}`}
      onClose={cerrar}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={cerrar}>Cancelar</Button>
          <Button type="submit" form="form-anular-compra" variant="danger" disabled={guardando}>
            {guardando ? 'Anulando…' : 'Confirmar anulación'}
          </Button>
        </>
      )}
    >
      <form id="form-anular-compra" className="close-modal-form" onSubmit={submit}>
        <p className="close-modal-lead">
          {requiereAdmin
            ? 'Esta compra ya ingresó stock. La anulación revierte el inventario y requiere credenciales de administrador.'
            : 'La orden pendiente se marcará como anulada sin afectar el inventario.'}
        </p>
        <label className="pay-field">
          <span>Motivo (mín. 5 caracteres)</span>
          <textarea
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            minLength={5}
            rows={3}
            required
          />
        </label>
        {requiereAdmin ? (
          <>
            <label className="pay-field">
              <span>Usuario administrador</span>
              <input value={adminUser} onChange={(e) => setAdminUser(e.target.value)} required />
            </label>
            <label className="pay-field">
              <span>Contraseña administrador</span>
              <input type="password" value={adminPass} onChange={(e) => setAdminPass(e.target.value)} required />
            </label>
          </>
        ) : null}
      </form>
    </Modal>
  );
}
