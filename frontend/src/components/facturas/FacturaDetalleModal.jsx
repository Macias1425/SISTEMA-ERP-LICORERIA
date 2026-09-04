import Modal from '../ui/Modal';
import FacturaDetalle from './FacturaDetalle';

export default function FacturaDetalleModal({ open, factura, negocio, onClose, onAnular, onImprimir }) {
  return (
    <Modal
      open={open}
      title={factura ? factura.numero : 'Detalle de factura'}
      subtitle="Comprobante de venta"
      onClose={onClose}
      size="md"
    >
      <FacturaDetalle
        factura={factura}
        negocio={negocio}
        onAnular={(item) => {
          onClose?.();
          onAnular?.(item);
        }}
        onImprimir={onImprimir}
      />
    </Modal>
  );
}
