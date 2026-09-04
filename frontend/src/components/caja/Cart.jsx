import Icon from '../ui/Icon';
import { dinero } from '../../utils/formato';

/** El precio autoritativo llega de la cotización; el del catálogo es solo respaldo visual. */
function precioUnitario(linea) {
  if (linea.precioUnitario !== undefined && linea.precioUnitario !== null) {
    return Number(linea.precioUnitario);
  }
  return Number(linea.producto.precioVenta) * (linea.presentacion.factorAUnidadMinima || 1);
}

export default function Cart({
  lineas,
  total = 0,
  ticketId = '000000',
  onCambiarCantidad,
  onQuitar,
}) {
  return (
    <section className="pos-ticket" aria-label="Ticket de venta">
      <header className="pos-ticket-head">
        <div className="pos-ticket-head-meta">
          <h2>Ticket de venta</h2>
          <span className="pos-ticket-lines">{lineas.length} líneas</span>
          <span className="pos-ticket-id">#{ticketId}</span>
        </div>
        <div className="pos-ticket-head-total">
          <small>Total a cobrar</small>
          <strong>{dinero(total)}</strong>
        </div>
      </header>

      <div className="pos-ticket-body">
        {!lineas.length ? (
          <div className="pos-ticket-empty">
            <Icon name="cart" size={32} strokeWidth={1.5} />
            <p>Escanee o busque un producto para comenzar</p>
          </div>
        ) : (
          <table className="pos-ticket-table">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">Código</th>
                <th scope="col">Descripción</th>
                <th scope="col">Cant</th>
                <th scope="col">Precio</th>
                <th scope="col">Importe</th>
                <th scope="col"><span className="visually-hidden">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {lineas.map((linea, index) => {
                const factor = linea.presentacion.factorAUnidadMinima || 1;
                const usadoOtros = lineas
                  .filter((item) => item.producto.id === linea.producto.id
                    && item.presentacion.id !== linea.presentacion.id)
                  .reduce(
                    (suma, item) => suma + item.cantidad * (item.presentacion.factorAUnidadMinima || 1),
                    0
                  );
                const maxCantidad = Math.floor(((linea.producto.stockActual ?? 0) - usadoOtros) / factor);
                const unitario = precioUnitario(linea);
                return (
                  <tr key={`${linea.producto.id}-${linea.presentacion.id}`} className={index % 2 ? 'is-alt' : ''}>
                    <td className="pos-ticket-num">{index + 1}</td>
                    <td className="pos-ticket-code">{linea.producto.codigo}</td>
                    <td className="pos-ticket-desc" title={linea.producto.nombre}>
                      {linea.producto.nombre}
                      {linea.aviso ? <small className="pos-ticket-aviso">{linea.aviso}</small> : null}
                    </td>
                    <td className="pos-ticket-qty">
                      <input
                        type="number"
                        min="1"
                        max={maxCantidad}
                        value={linea.cantidad}
                        aria-label={`Cantidad de ${linea.producto.nombre}`}
                        onChange={(event) => {
                          const valor = Number(event.target.value);
                          if (Number.isFinite(valor)) {
                            onCambiarCantidad(linea, valor);
                          }
                        }}
                      />
                    </td>
                    <td className="pos-ticket-unit">{dinero(unitario)}</td>
                    <td className="pos-ticket-amount">{dinero(linea.subtotal)}</td>
                    <td className="pos-ticket-acc">
                      <button
                        type="button"
                        className="pos-ticket-remove"
                        onClick={() => onQuitar(linea)}
                        aria-label={`Quitar ${linea.producto.nombre}`}
                      >
                        <Icon name="trash" size={16} strokeWidth={2} />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
