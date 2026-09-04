import Button from '../ui/Button';
import Icon from '../ui/Icon';
import Modal from '../ui/Modal';
import CompraDetalle from './CompraDetalle';

export default function CompraDetalleModal({
  open,
  compra,
  onClose,
  puedeGestionar,
  guardando = false,
  onRecibirOrden,
  onRecibirDirecta,
  onAnular,
  onCerrarOrden,
}) {
  const puedeRecibir = puedeGestionar && compra?.recibible;
  const puedeAnular = puedeGestionar && compra?.anulable;
  const puedeCerrar = puedeGestionar && compra?.cerrable;

  return (
    <Modal
      open={open && Boolean(compra)}
      title={compra?.numero || 'Detalle de compra'}
      subtitle={compra?.proveedorNombre || 'Orden / recepción'}
      onClose={onClose}
      size="lg"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cerrar</Button>
          {puedeGestionar ? (
            <Button type="button" variant="secondary" onClick={onRecibirDirecta}>
              <Icon name="plus" size={16} strokeWidth={2.5} />
              Nueva recepción
            </Button>
          ) : null}
          {puedeCerrar ? (
            <Button type="button" variant="secondary" onClick={() => onCerrarOrden?.(compra)} disabled={guardando}>
              Cerrar incompleta
            </Button>
          ) : null}
          {puedeAnular ? (
            <Button type="button" variant="danger" onClick={() => onAnular?.(compra)} disabled={guardando}>
              <Icon name="ban" size={16} strokeWidth={2} />
              Anular
            </Button>
          ) : null}
          {puedeRecibir ? (
            <Button type="button" onClick={() => onRecibirOrden?.(compra)} disabled={guardando}>
              <Icon name="check" size={16} strokeWidth={2.5} />
              {guardando ? 'Procesando…' : 'Recibir mercancía'}
            </Button>
          ) : null}
        </>
      )}
    >
      <CompraDetalle compra={compra} compact />
    </Modal>
  );
}
