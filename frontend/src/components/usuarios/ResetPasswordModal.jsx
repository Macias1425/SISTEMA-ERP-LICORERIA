import { useEffect, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function ResetPasswordModal({ open, usuario, guardando, onClose, onConfirmar }) {
  const [passwordNueva, setPasswordNueva] = useState('');

  useEffect(() => {
    if (open) {
      setPasswordNueva('');
    }
  }, [open, usuario?.id]);

  function submit(event) {
    event.preventDefault();
    onConfirmar?.(passwordNueva);
  }

  return (
    <Modal
      open={open}
      subtitle="Asigna una clave temporal que el usuario cambiará al entrar"
      title={usuario ? `Resetear clave — ${usuario.username}` : 'Resetear clave'}
      onClose={onClose}
      size="sm"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-reset-password" disabled={guardando || !usuario?.activo}>
            {guardando ? 'Guardando…' : 'Asignar clave'}
          </Button>
        </>
      )}
    >
      <form id="form-reset-password" className="close-modal-form" onSubmit={submit}>
        <label>
          Nueva contraseña temporal
          <input
            type="password"
            value={passwordNueva}
            onChange={(event) => setPasswordNueva(event.target.value)}
            minLength={8}
            autoComplete="new-password"
            required
          />
        </label>
        <p className="hint">
          Mínimo 8 caracteres, con letras y números. No puede ser igual al nombre de usuario.
        </p>
      </form>
    </Modal>
  );
}
