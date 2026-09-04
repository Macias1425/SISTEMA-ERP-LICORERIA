import Modal from '../ui/Modal';
import UsuarioDetalle from './UsuarioDetalle';

export default function UsuarioDetalleModal({
  open,
  usuario,
  guardando,
  onClose,
  onEditar,
  onResetPassword,
  onToggleActivo,
}) {
  return (
    <Modal
      open={open && Boolean(usuario)}
      title={usuario?.nombreCompleto || 'Usuario'}
      subtitle={usuario ? `@${usuario.username}` : undefined}
      onClose={onClose}
      size="lg"
    >
      <UsuarioDetalle
        usuario={usuario}
        guardando={guardando}
        onEditar={(item) => {
          onClose?.();
          onEditar?.(item);
        }}
        onResetPassword={(item) => {
          onClose?.();
          onResetPassword?.(item);
        }}
        onToggleActivo={onToggleActivo}
      />
    </Modal>
  );
}
