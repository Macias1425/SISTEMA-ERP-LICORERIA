import { useEffect, useState } from 'react';
import Button from './Button';
import Modal from './Modal';

export default function SupervisorAuthModal({
  open,
  titulo = 'Autorización requerida',
  mensaje,
  guardando,
  onClose,
  onConfirmar,
}) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorLocal, setErrorLocal] = useState('');

  useEffect(() => {
    if (open) {
      setUsername('');
      setPassword('');
      setErrorLocal('');
    }
  }, [open]);

  function submit(event) {
    event.preventDefault();
    if (!username.trim() || !password) {
      setErrorLocal('Ingrese usuario y contraseña de administrador.');
      return;
    }
    onConfirmar?.({ username: username.trim(), password });
  }

  return (
    <Modal
      open={open}
      title={titulo}
      subtitle="Credenciales de administrador"
      onClose={onClose}
      size="sm"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-supervisor-auth" disabled={guardando}>
            {guardando ? 'Validando…' : 'Autorizar'}
          </Button>
        </>
      )}
    >
      <form id="form-supervisor-auth" className="close-modal-form" onSubmit={submit}>
        {errorLocal ? <p className="pos-alert" role="alert">{errorLocal}</p> : null}
        <p className="close-modal-lead">{mensaje}</p>
        <div className="pay-block">
          <label className="pay-field">
            <span>Usuario administrador</span>
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
          </label>
          <label className="pay-field">
            <span>Contraseña</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
          </label>
        </div>
      </form>
    </Modal>
  );
}
