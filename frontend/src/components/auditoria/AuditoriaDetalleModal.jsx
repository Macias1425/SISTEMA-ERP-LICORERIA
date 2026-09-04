import Button from '../ui/Button';
import Modal from '../ui/Modal';
import AuditoriaEventoChip from './AuditoriaEventoChip';
import AuditoriaRiesgoChip from './AuditoriaRiesgoChip';

function fechaHora(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function bloqueJson(titulo, valor) {
  if (!valor) return null;
  let texto = valor;
  try {
    texto = JSON.stringify(JSON.parse(valor), null, 2);
  } catch {
    texto = valor;
  }
  return (
    <div className="aud-detalle-bloque">
      <h4>{titulo}</h4>
      <pre>{texto}</pre>
    </div>
  );
}

export default function AuditoriaDetalleModal({ open, evento, onClose }) {
  return (
    <Modal
      open={open && Boolean(evento)}
      title={evento?.eventoEtiqueta || evento?.accion || 'Detalle del evento'}
      subtitle={evento ? `Evento #${evento.id}` : undefined}
      onClose={onClose}
      size="lg"
      footer={(
        <>
          <p className="hint">Los registros de auditoría son inmutables y no pueden editarse ni eliminarse.</p>
          <Button type="button" variant="secondary" onClick={onClose}>Cerrar</Button>
        </>
      )}
    >
      {evento ? (
        <>
          <div className="aud-detalle-grid">
            <div>
              <span>Fecha y hora</span>
              <strong>{fechaHora(evento.fechaHora)}</strong>
            </div>
            <div>
              <span>Usuario</span>
              <strong>{evento.usuarioNombre || evento.rol || '—'}</strong>
            </div>
            <div>
              <span>Evento</span>
              <AuditoriaEventoChip
                etiqueta={evento.eventoEtiqueta}
                codigo={evento.codigoEvento || evento.accion}
              />
            </div>
            <div>
              <span>Riesgo</span>
              <AuditoriaRiesgoChip nivel={evento.nivelRiesgo} />
            </div>
            <div>
              <span>Módulo</span>
              <strong>{evento.modulo || '—'}</strong>
              <small>{evento.tablaEntidad || evento.entidad || '—'}</small>
            </div>
            <div>
              <span>Registro</span>
              <strong>{evento.entidadId != null ? `#${evento.entidadId}` : '—'}</strong>
            </div>
            <div>
              <span>IP origen</span>
              <strong>{evento.ip || '—'}</strong>
            </div>
          </div>

          {evento.detalle ? (
            <div className="aud-detalle-bloque">
              <h4>Detalle operativo</h4>
              <p>{evento.detalle}</p>
            </div>
          ) : null}

          {bloqueJson('Valor anterior', evento.valorAnterior)}
          {bloqueJson('Valor nuevo', evento.valorNuevo)}
        </>
      ) : null}
    </Modal>
  );
}
