import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function ConfirmacionModal({
  open,
  titulo,
  mensaje,
  confirmarLabel = 'Confirmar',
  peligro = false,
  procesando = false,
  onCerrar,
  onConfirmar,
}) {
  return (
    <Modal
      open={open}
      title={titulo}
      onClose={procesando ? undefined : onCerrar}
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onCerrar} disabled={procesando}>
            Cancelar
          </Button>
          <Button
            type="button"
            variant={peligro ? 'danger' : 'primary'}
            onClick={onConfirmar}
            disabled={procesando}
          >
            {procesando ? 'Procesando…' : confirmarLabel}
          </Button>
        </>
      )}
    >
      <p className="mnt-confirm-text">{mensaje}</p>
    </Modal>
  );
}
