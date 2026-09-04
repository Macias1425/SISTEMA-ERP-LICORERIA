import { Link } from 'react-router-dom';

import EmptyState from '../ui/EmptyState';

import Icon from '../ui/Icon';

import BlockedActionNote from '../ui/BlockedActionNote';

import CompraEstadoChip from './CompraEstadoChip';
import ImpactoRecepcionPanel from './ImpactoRecepcionPanel';
import { dinero } from '../../utils/formato';






function fecha(valor) {

  if (!valor) return '—';

  return new Date(valor).toLocaleString('es-NI');

}



export default function CompraDetalle({ compra, compact = false }) {

  if (!compra) {

    return (

      <EmptyState

        className="fac-detail-empty"

        icon="truck"

        title="Seleccione una compra"

        message="Elija una orden o recepción del listado para ver proveedor, líneas y totales."

      />

    );

  }



  return (

    <article className={`fac-detail com-detail${compact ? ' com-detail-compact' : ''}`}>

      <header className="fac-detail-head">

        <div>

          <p className="brand-kicker">Compra</p>

          <h2>{compra.numero}</h2>

          <p className="fac-detail-meta">{fecha(compra.fecha)}</p>

        </div>

        <CompraEstadoChip estado={compra.estado} />

      </header>



      <div className="fac-detail-grid com-detail-grid">

        <div><span>Proveedor</span><strong>{compra.proveedorNombre}</strong></div>

        <div><span>Documento</span><strong>{compra.documentoProveedor || '—'}</strong></div>

        <div><span>Registró</span><strong>{compra.usuarioNombre || '—'}</strong></div>

        {compra.observacion ? (

          <div className="com-detail-obs"><span>Observación</span><strong>{compra.observacion}</strong></div>

        ) : null}

      </div>



      {(compra.detalles || []).length ? (

        <div className="com-lines-wrap">

          <table className="com-lines-table">

            <thead>

              <tr>

                <th>Producto</th>

                <th>Ordenada</th>
                <th>Recibida</th>
                <th>Rechaz.</th>
                <th>Pend.</th>
                <th>Botellas</th>

                <th>Costo u.</th>

                <th>Importe</th>

              </tr>

            </thead>

            <tbody>

              {(compra.detalles || []).map((detalle, idx) => (

                <tr key={`${detalle.productoId}-${detalle.presentacionId}-${idx}`}>

                  <td>

                    <strong>{detalle.productoNombre}</strong>

                    {detalle.presentacionNombre ? (

                      <small>{detalle.presentacionNombre}</small>

                    ) : null}

                    {detalle.fechaVencimiento ? (

                      <small className="com-line-vence">Vence {detalle.fechaVencimiento}</small>

                    ) : null}

                    {detalle.notasQc ? (
                      <small className="com-line-qc">QC: {detalle.notasQc}</small>
                    ) : null}
                  </td>
                  <td>{detalle.cantidadOrdenada ?? detalle.cantidad}</td>
                  <td>{detalle.cantidadRecibida ?? 0}</td>
                  <td>{detalle.cantidadRechazada ?? 0}</td>
                  <td>{detalle.cantidadPendiente ?? 0}</td>
                  <td>{detalle.cantidadUmm ?? '—'}</td>

                  <td>{dinero(detalle.costoUnitario)}</td>

                  <td>{dinero(detalle.subtotal)}</td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>

      ) : (

        <EmptyState icon="package" title="Sin líneas" message="Esta compra no tiene detalle registrado." compact />

      )}



      <dl className="pos-totals fac-totals com-totals">

        <div className="pos-total-row"><dt>Total compra</dt><dd>{dinero(compra.total)}</dd></div>

      </dl>



      {(compra.estado === 'PENDIENTE' || compra.estado === 'PARCIAL') ? (
        <p className="com-rn-hint">
          <Icon name="clipboard" size={14} strokeWidth={2} />
          {compra.estado === 'PARCIAL'
            ? ' Recepción parcial: confirme el saldo pendiente o cierre la orden si el proveedor no enviará el resto.'
            : ' Orden pendiente: el stock se actualiza al confirmar la recepción.'}
        </p>
      ) : null}

      <ImpactoRecepcionPanel impactos={compra.impactosRecepcion} />



      {!compra.recibible && compra.motivoNoRecibible ? (

        <BlockedActionNote motivo={compra.motivoNoRecibible} />

      ) : null}

      {!compra.anulable && compra.motivoNoAnulable ? (

        <BlockedActionNote motivo={compra.motivoNoAnulable} />

      ) : null}



      {!compact ? (

        <p className="hint com-detail-link">

          <Link to="/proveedores">Ver catálogo de proveedores</Link>

          {' · '}

          <Link to="/inventario">Kardex de inventario</Link>

        </p>

      ) : null}

    </article>

  );

}


