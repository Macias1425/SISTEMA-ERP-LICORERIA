import EmptyState from '../ui/EmptyState';
import MermaEstadoChip from './MermaEstadoChip';
import { dinero } from '../../utils/formato';

function fecha(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI');
}

export default function CompraDetalle({ compra, onRecibir }) {
  if (!compra) {
    return (
      <EmptyState
        className="fac-detail-empty"
        icon="package"
        title="Seleccione una compra"
        message="Elija una recepción del listado para ver proveedor, líneas y totales."
      />
    );
  }

  return (
    <article className="fac-detail">
      <header className="fac-detail-head">
        <div>
          <p className="brand-kicker">Recepción</p>
          <h2>{compra.numero}</h2>
          <p className="fac-detail-meta">{fecha(compra.fecha)}</p>
        </div>
        <span className="fac-estado fac-estado-ok">{compra.estado || 'RECIBIDA'}</span>
      </header>

      <div className="fac-detail-grid">
        <div><span>Proveedor</span><strong>{compra.proveedorNombre}</strong></div>
        {compra.documentoProveedor ? (
          <div><span>Documento</span><strong>{compra.documentoProveedor}</strong></div>
        ) : null}
        <div><span>Registró</span><strong>{compra.usuarioNombre || '—'}</strong></div>
        {compra.observacion ? (
          <div><span>Observación</span><strong>{compra.observacion}</strong></div>
        ) : null}
      </div>

      <ul className="receipt-lines fac-lines">
        {(compra.detalles || []).map((detalle) => (
          <li key={`${detalle.productoId}-${detalle.presentacionId}`}>
            <span>
              {detalle.cantidad} × {detalle.productoNombre}
              {detalle.presentacionNombre ? ` (${detalle.presentacionNombre})` : ''}
              {detalle.fechaVencimiento ? ` · vence ${detalle.fechaVencimiento}` : ''}
            </span>
            <span>{dinero(detalle.subtotal)}</span>
          </li>
        ))}
      </ul>

      <dl className="pos-totals fac-totals">
        <div className="pos-total-row"><dt>Total</dt><dd>{dinero(compra.total)}</dd></div>
      </dl>

      {onRecibir ? (
        <footer className="fac-detail-actions no-print">
          <button type="button" className="btn primary" onClick={onRecibir}>Nueva recepción</button>
        </footer>
      ) : null}
    </article>
  );
}
