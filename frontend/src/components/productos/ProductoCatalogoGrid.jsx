import Icon from '../ui/Icon';
import ProductoEstadoChip from './ProductoEstadoChip';
import ProductoImagen from './ProductoImagen';
import { dinero } from '../../utils/formato';

export default function ProductoCatalogoGrid({ productos, onVer, modoCatalogo = false }) {
  return (
    <div className="prod-grid" role="list">
      {productos.map((producto) => (
        <article
          key={producto.id}
          className={`prod-card${modoCatalogo ? ' prod-card-catalogo' : ''}`}
          role="listitem"
        >
          <button type="button" className="prod-card-main" onClick={() => onVer(producto)}>
            <ProductoImagen producto={producto} size="lg" className="prod-card-img" />
            <div className="prod-card-body">
              <span className="cat-code">{producto.codigo}</span>
              <strong className="prod-card-name">{producto.nombre}</strong>
              <span className="prod-card-meta">
                {producto.marca || 'Sin marca'} · {producto.categoriaNombre || 'Sin categoría'}
              </span>
              <div className="prod-card-prices">
                {modoCatalogo ? (
                  <strong>Venta {dinero(producto.precioVenta)}</strong>
                ) : (
                  <>
                    <span>Compra {dinero(producto.precioCompra)}</span>
                    <strong>Venta {dinero(producto.precioVenta)}</strong>
                  </>
                )}
              </div>
              {!modoCatalogo ? (
                <div className="prod-card-foot">
                  <span className={`cat-badge${producto.nivelAlerta === 'OK' ? ' cat-badge-ok' : producto.nivelAlerta ? ' cat-badge-warn' : ' cat-badge-muted'}`}>
                    {producto.stockActual ?? 0} UMM
                  </span>
                  <ProductoEstadoChip activo={producto.activo} nivelAlerta={producto.nivelAlerta} />
                </div>
              ) : (
                <div className="prod-card-foot">
                  <span className={`cat-badge${(producto.stockActual ?? 0) > 0 ? ' cat-badge-ok' : ' cat-badge-warn'}`}>
                    {(producto.stockActual ?? 0) > 0 ? 'Disponible' : 'Agotado'}
                  </span>
                </div>
              )}
            </div>
          </button>
          <button
            type="button"
            className="prod-card-action"
            title="Ver detalle"
            onClick={() => onVer(producto)}
          >
            <Icon name="eye" size={16} strokeWidth={2} />
          </button>
        </article>
      ))}
    </div>
  );
}
