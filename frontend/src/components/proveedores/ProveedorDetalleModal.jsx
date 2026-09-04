import Modal from '../ui/Modal';
import ProveedorDetalle from './ProveedorDetalle';

export default function ProveedorDetalleModal({
  open,
  proveedor,
  precios,
  paginaPrecios = 0,
  paginaMetaPrecios,
  onPaginaPrecios,
  onClose,
  onEditar,
  onPrecio,
}) {
  return (
    <Modal
      open={open && Boolean(proveedor)}
      title={proveedor?.nombre || 'Proveedor'}
      subtitle={proveedor?.documento || 'Catálogo y contacto'}
      onClose={onClose}
      size="lg"
    >
      <ProveedorDetalle
        proveedor={proveedor}
        precios={precios}
        pagina={paginaPrecios}
        paginaMeta={paginaMetaPrecios}
        onPagina={onPaginaPrecios}
        onEditar={(item) => {
          onClose?.();
          onEditar?.(item);
        }}
        onPrecio={() => {
          onClose?.();
          onPrecio?.();
        }}
      />
    </Modal>
  );
}
