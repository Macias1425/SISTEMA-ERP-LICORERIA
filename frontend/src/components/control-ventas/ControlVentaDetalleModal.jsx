import { useEffect, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import Button from '../ui/Button';
import Modal from '../ui/Modal';
import { controlVentasService } from '../../services/controlVentasService';
import { fechaHora, moneda as formatoMoneda } from '../../utils/formato';

const TIPO_LABEL = {
  ALTO_MONTO: 'Venta de alto monto',
  OVERRIDE_PRECIO: 'Cambio de precio en POS',
  SUPERVISOR_AUTORIZADO: 'Autorización de administrador',
  ANULACION: 'Anulación de factura',
};

function badgeClass(nivel) {
  const val = (nivel || 'medio').toLowerCase();
  return `cv-badge cv-badge-${val}`;
}

export default function ControlVentaDetalleModal({ evento, onClose }) {
  const [venta, setVenta] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!evento) {
      setVenta(null);
      setError('');
      return;
    }
    if (evento.tipo === 'ANULACION' || !evento.entidadId) {
      setVenta(null);
      return;
    }
    let cancelado = false;
    (async () => {
      setCargando(true);
      setError('');
      try {
        const detalle = await controlVentasService.detalleVenta(evento.entidadId);
        if (!cancelado) setVenta(detalle);
      } catch (err) {
        if (!cancelado) setError(mensajeError(err));
      } finally {
        if (!cancelado) setCargando(false);
      }
    })();
    return () => { cancelado = true; };
  }, [evento]);

  return (
    <Modal
      open={Boolean(evento)}
      title={evento ? (TIPO_LABEL[evento.tipo] || evento.tipo) : ''}
      subtitle={evento ? `${evento.referencia} · ${fechaHora(evento.fecha)}` : ''}
      onClose={onClose}
      size="lg"
      footer={(
        <Button type="button" variant="secondary" onClick={onClose}>Cerrar</Button>
      )}
    >
      {evento ? (
        <div className="cv-detalle-body">
          <div className="cv-detalle-chips">
            <span className={badgeClass(evento.nivel)}>{evento.nivel || 'medio'}</span>
            <span className="cv-detalle-chip">{evento.cajeroNombre || 'Sin cajero'}</span>
            <span className="cv-detalle-chip cv-detalle-monto">{formatoMoneda(evento.monto)}</span>
          </div>

          {evento.detalle ? (
            <p className="cv-detalle-nota">{evento.detalle}</p>
          ) : null}

          {cargando ? <p className="placeholder">Cargando venta…</p> : null}
          {error ? <p className="pos-alert">{error}</p> : null}

          {venta ? (
            <section className="cv-detalle-venta">
              <h3>Venta {venta.numero}</h3>
              <dl className="cv-detalle-grid">
                <div><dt>Total</dt><dd>{formatoMoneda(venta.total)}</dd></div>
                <div><dt>Pago</dt><dd>{venta.formaPago || '—'}</dd></div>
                <div><dt>Cliente</dt><dd>{venta.clienteNombre || 'Consumidor final'}</dd></div>
                <div><dt>IVA</dt><dd>{formatoMoneda(venta.impuesto)}</dd></div>
              </dl>
              {venta.detalles?.length ? (
                <div className="cv-table-wrap">
                  <table className="data-table cv-table">
                    <thead>
                      <tr>
                        <th>Producto</th>
                        <th>Cant.</th>
                        <th>Precio</th>
                        <th>Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {venta.detalles.map((linea) => (
                        <tr key={`${linea.productoId}-${linea.presentacionId}`}>
                          <td>{linea.productoNombre}</td>
                          <td>{linea.cantidad}</td>
                          <td>{formatoMoneda(linea.precioUnitario)}</td>
                          <td>{formatoMoneda(linea.subtotal)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </section>
          ) : null}
        </div>
      ) : null}
    </Modal>
  );
}
