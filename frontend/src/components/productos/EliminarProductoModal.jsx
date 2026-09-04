import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function EliminarProductoModal({ open, producto, guardando, onClose, onConfirmar }) {
  return (
    <Modal
      open={open}
      subtitle="Solo admin · sin stock"
      title={`Eliminar ${producto?.codigo || 'producto'}`}
      onClose={onClose}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="button" variant="danger" disabled={guardando} onClick={onConfirmar}>
            {guardando ? 'Eliminando…' : 'Confirmar eliminación'}
          </Button>
        </>
      )}
    >
      <p className="close-modal-lead">
        Se borrará <strong>{producto?.nombre}</strong> del catálogo. Esta acción queda en auditoría.
      </p>
    </Modal>
  );
}
