import EmptyState from '../ui/EmptyState';
import MermaEstadoChip from './MermaEstadoChip';

function fecha(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI');
}

const TIPOS = {
  ROTURA: 'Rotura',
  DANADA: 'Dañada',
  MUESTRA: 'Muestra',
};

export default function MermaDetalle({ merma, onAprobar, onRechazar, onSolicitar }) {
  if (!merma) {
    return (
      <EmptyState
        className="fac-detail-empty"
        icon="trendDown"
        title="Seleccione una merma"
        message="Consulte solicitudes pendientes o registre una nueva salida por merma."
      />
    );
  }

  return (
    <article className="fac-detail">
      <header className="fac-detail-head">
        <div>
          <p className="brand-kicker">Salida por merma</p>
          <h2>#{merma.id}</h2>
          <p className="fac-detail-meta">{fecha(merma.fechaSolicitud)}</p>
        </div>
        <MermaEstadoChip estado={merma.estado} />
      </header>

      <div className="fac-detail-grid">
        <div><span>Producto</span><strong>{merma.productoNombre || `#${merma.productoId}`}</strong></div>
        <div><span>Presentación</span><strong>{merma.presentacionNombre || '—'}</strong></div>
        <div><span>Cantidad</span><strong>{merma.cantidadPresentacion} ({merma.cantidadUmm} UMM)</strong></div>
        <div><span>Tipo</span><strong>{TIPOS[merma.tipo] || merma.tipo}</strong></div>
        <div><span>Solicitó</span><strong>{merma.solicitadoPorNombre || '—'}</strong></div>
        {merma.autorizadoPorNombre ? (
          <div><span>Resolvió</span><strong>{merma.autorizadoPorNombre}</strong></div>
        ) : null}
      </div>

      <div className="fac-anulada-box">
        <strong>Motivo</strong>
        <p>{merma.motivo}</p>
        {merma.fechaResolucion ? (
          <p className="hint">Resuelta el {fecha(merma.fechaResolucion)}</p>
        ) : null}
      </div>

      <footer className="fac-detail-actions no-print">
        {onSolicitar ? (
          <button type="button" className="btn secondary" onClick={onSolicitar}>Nueva solicitud</button>
        ) : null}
        {merma.aprobable ? (
          <button type="button" className="btn primary" onClick={() => onAprobar?.(merma)}>Aprobar</button>
        ) : null}
        {merma.rechazable ? (
          <button type="button" className="btn danger" onClick={() => onRechazar?.(merma)}>Rechazar</button>
        ) : null}
        {merma.estado === 'PENDIENTE' && !merma.aprobable ? (
          <p className="pay-note warn">{merma.motivoNoResolucion}</p>
        ) : null}
      </footer>
    </article>
  );
}
