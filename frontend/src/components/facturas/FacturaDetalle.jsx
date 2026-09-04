import EmptyState from '../ui/EmptyState';
import FacturaTicket, { imprimirRecibo } from '../caja/FacturaTicket';
import FacturaEstadoChip from './FacturaEstadoChip';

function fecha(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI');
}

export default function FacturaDetalle({ factura, negocio, onAnular, onImprimir }) {
  if (!factura) {
    return (
      <EmptyState
        className="fac-detail-empty"
        icon="receipt"
        title="Seleccione una factura"
        message="Elija un comprobante del listado para ver el detalle, imprimir o anular."
      />
    );
  }

  function imprimir() {
    if (onImprimir) {
      onImprimir(factura);
      return;
    }
    imprimirRecibo();
  }

  return (
    <article className="fac-detail fac-detail-recibo">
      <header className="fac-detail-head no-print">
        <div>
          <p className="brand-kicker">Comprobante de venta</p>
          <h2>{factura.numeroFiscal || factura.numero}</h2>
        </div>
        <FacturaEstadoChip estado={factura.estado} />
      </header>

      <FacturaTicket
        factura={factura}
        negocio={negocio}
        mostrarAcciones={false}
      />

      {factura.estado === 'ANULADA' ? (
        <div className="fac-anulada-box no-print">
          <strong>Factura anulada el {fecha(factura.fechaAnulacion)}</strong>
          <p>Motivo de anulación: {factura.motivoAnulacion || 'No indicado'}</p>
          <p className="hint">
            Solicitó: {factura.solicitadoAnulacionPorNombre || 'No registrado'}
            {factura.anuladoPorNombre ? ` · Autorizó: ${factura.anuladoPorNombre}` : ''}
          </p>
        </div>
      ) : null}

      <footer className="fac-detail-actions no-print">
        <button type="button" className="btn secondary" onClick={imprimir}>Imprimir recibo</button>
        {factura.anulable ? (
          <button type="button" className="btn danger" onClick={() => onAnular?.(factura)}>Anular factura</button>
        ) : factura.estado === 'EMITIDA' ? (
          <p className="pay-note warn">{factura.motivoNoAnulable}</p>
        ) : null}
      </footer>
    </article>
  );
}
