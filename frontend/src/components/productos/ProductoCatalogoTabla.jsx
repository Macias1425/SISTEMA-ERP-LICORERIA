import Icon from '../ui/Icon';
import ProductoEstadoChip from './ProductoEstadoChip';
import ProductoImagen from './ProductoImagen';
import { dinero } from '../../utils/formato';

export default function ProductoCatalogoTabla({ productos, onVer, modoCatalogo = false }) {
  return (
    <div className="fac-table-wrap">
      <table className="data-table fac-table cat-table prod-table">
        <thead>
          <tr>
            <th>Foto</th>
            <th>Producto</th>
            <th>Categoría</th>
            {modoCatalogo ? null : <th>Costo</th>}
            <th>Precio venta</th>
            <th>{modoCatalogo ? 'Disponibilidad' : 'Stock'}</th>
            {modoCatalogo ? null : <th>Estado</th>}
            <th className="no-print">Acciones</th>
          </tr>
        </thead>
        <tbody>
          {productos.map((producto) => (
            <tr key={producto.id} className="prod-row-click" onClick={() => onVer(producto)}>
              <td className="prod-table-img">
                <ProductoImagen producto={producto} size="sm" />
              </td>
              <td>
                <div className="cat-product-cell">
                  <strong>{producto.nombre}</strong>
                  <span className="cat-code">{producto.codigo}</span>
                  {modoCatalogo && producto.marca ? <small>{producto.marca}</small> : null}
                </div>
              </td>
              <td>
                <span className="cat-badge">
                  {producto.categoriaNombre || 'Sin categoría'}
                </span>
              </td>
              {modoCatalogo ? null : <td>{dinero(producto.precioCompra)}</td>}
              <td>{dinero(producto.precioVenta)}</td>
              <td>
                {modoCatalogo ? (
                  <span className={`cat-badge${(producto.stockActual ?? 0) > 0 ? ' cat-badge-ok' : ' cat-badge-warn'}`}>
                    {(producto.stockActual ?? 0) > 0 ? 'Disponible' : 'Agotado'}
                  </span>
                ) : (
                  <span className={`cat-badge${producto.nivelAlerta === 'OK' ? ' cat-badge-ok' : producto.nivelAlerta ? ' cat-badge-warn' : ' cat-badge-muted'}`}>
                    {producto.stockActual ?? 0} UMM
                  </span>
                )}
              </td>
              {modoCatalogo ? null : (
                <td><ProductoEstadoChip activo={producto.activo} nivelAlerta={producto.nivelAlerta} /></td>
              )}
              <td className="no-print" onClick={(e) => e.stopPropagation()}>
                <button type="button" className="fca-act fca-act-detalle" title="Ver detalle" onClick={() => onVer(producto)}>
                  <Icon name="eye" size={17} strokeWidth={2} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
