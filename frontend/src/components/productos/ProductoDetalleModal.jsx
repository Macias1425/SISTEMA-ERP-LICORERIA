import Modal from '../ui/Modal';
import ProductoDetalle from './ProductoDetalle';

export default function ProductoDetalleModal({
  open,
  producto,
  esAdmin,
  soloCatalogo = false,
  puedeGestionar = true,
  onClose,
  onEditar,
  onPresentacion,
  onEditarPresentacion,
  onEliminar,
}) {
  return (
    <Modal
      open={open && Boolean(producto)}
      title={producto?.nombre || 'Detalle de producto'}
      subtitle={producto?.codigo}
      onClose={onClose}
      size="lg"
    >
      <ProductoDetalle
        producto={producto}
        esAdmin={esAdmin}
        soloCatalogo={soloCatalogo}
        puedeGestionar={puedeGestionar}
        onEditar={(item) => {
          onClose?.();
          onEditar?.(item);
        }}
        onPresentacion={(item) => {
          onPresentacion?.(item);
        }}
        onEditarPresentacion={(presentacion) => {
          onClose?.();
          onEditarPresentacion?.(presentacion);
        }}
        onEliminar={(item) => {
          onClose?.();
          onEliminar?.(item);
        }}
      />
    </Modal>
  );
}
