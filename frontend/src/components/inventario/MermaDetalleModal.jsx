import Modal from '../ui/Modal';
import MermaDetalle from './MermaDetalle';

export default function MermaDetalleModal({
  open,
  merma,
  onClose,
  onSolicitar,
  onAprobar,
  onRechazar,
}) {
  return (
    <Modal
      open={open && Boolean(merma)}
      title={merma ? `Merma #${merma.id}` : 'Detalle de merma'}
      subtitle="Salida por merma"
      onClose={onClose}
      size="lg"
    >
      <MermaDetalle
        merma={merma}
        onSolicitar={() => {
          onClose?.();
          onSolicitar?.();
        }}
        onAprobar={() => {
          onClose?.();
          onAprobar?.(merma);
        }}
        onRechazar={() => {
          onClose?.();
          onRechazar?.(merma);
        }}
      />
    </Modal>
  );
}
