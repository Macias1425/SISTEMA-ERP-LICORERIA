import { useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function AnulacionModal({ open, factura, guardando, titulo, onClose, onConfirmar }) {
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
    onConfirmar?.({
      motivo: motivo.trim(),
      autorizacion: { username: adminUser.trim(), password: adminPass },
    });
  }

  return (
    <Modal
      open={open}
      subtitle="Anulación de comprobante"
      title={titulo || `Anular ${factura?.numero || 'factura'}`}
      onClose={cerrar}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={cerrar}>Cancelar</Button>
          <Button type="submit" form="form-anular-factura" variant="danger" disabled={guardando}>
            {guardando ? 'Anulando…' : 'Confirmar anulación'}
          </Button>
        </>
      )}
    >
      <form id="form-anular-factura" className="close-modal-form" onSubmit={submit}>
        <p className="close-modal-lead">
          Esta acción anula el comprobante, revierte el stock y deja la venta fuera del cierre de caja.
          {' '}
          Requiere credenciales de un administrador.
        </p>
        {factura?.motivoNoAnulable ? (
          <p className="pos-alert">{factura.motivoNoAnulable}</p>
        ) : null}
        <div className="pay-block">
          <label className="pay-field">
            <span>Motivo (mín. 5 caracteres)</span>
            <textarea
              value={motivo}
              onChange={(event) => setMotivo(event.target.value)}
              minLength={5}
              rows={3}
              required
              placeholder="Ej. cliente desistió, error de cobro…"
            />
          </label>
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
