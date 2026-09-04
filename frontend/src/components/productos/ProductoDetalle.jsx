import EmptyState from '../ui/EmptyState';
import ProductoImagen from './ProductoImagen';
import ProductoEstadoChip from './ProductoEstadoChip';
import { dinero } from '../../utils/formato';
import { etiquetaPolitica } from '../../utils/politicaPrecio';

export default function ProductoDetalle({
  producto,
  onEditar,
  onPresentacion,
  onEditarPresentacion,
  onEliminar,
  esAdmin,
  soloCatalogo = false,
  puedeGestionar = true,
}) {
  if (!producto) {
    return (
      <EmptyState
        className="fac-detail-empty"
        icon="wine"
        title="Seleccione un producto"
        message={soloCatalogo
          ? 'Consulte la ficha comercial del producto.'
          : 'Consulte stock, precios, presentaciones y alertas del catálogo.'}
      />
    );
  }

  const margen = Number(producto.precioVenta || 0) - Number(producto.precioCompra || 0);
  const presentaciones = (producto.presentaciones || []).filter((p) => soloCatalogo ? p.activo !== false : true);

  return (
    <article className="fac-detail prod-detail">
      <header className="fac-detail-head prod-detail-head">
        <ProductoImagen producto={producto} size="lg" className="prod-detail-photo" />
        <div>
          <p className="brand-kicker">{producto.codigo}</p>
          <h2>{producto.nombre}</h2>
          <p className="fac-detail-meta">{producto.marca || 'Sin marca'} · {producto.categoriaNombre || 'Sin categoría'}</p>
        </div>
        {!soloCatalogo ? (
          <ProductoEstadoChip activo={producto.activo} nivelAlerta={producto.nivelAlerta} />
        ) : null}
      </header>

      <div className="fac-detail-grid">
        {soloCatalogo ? (
          <>
            <div><span>Precio venta</span><strong>{dinero(producto.precioVenta)}</strong></div>
            <div><span>Alcohólico</span><strong>{producto.esAlcoholico ? 'Sí' : 'No'}</strong></div>
            <div><span>Disponible</span><strong>{(producto.stockActual ?? 0) > 0 ? 'Sí' : 'Agotado'}</strong></div>
          </>
        ) : (
          <>
            <div><span>Stock (UMM)</span><strong>{producto.stockActual ?? 0}</strong></div>
            <div><span>Mínimo</span><strong>{producto.stockMinimo ?? 0}</strong></div>
            <div><span>Crítico</span><strong>{producto.stockCritico ?? 0}</strong></div>
            <div><span>Compra</span><strong>{dinero(producto.precioCompra)}</strong></div>
            <div><span>Venta</span><strong>{dinero(producto.precioVenta)}</strong></div>
            <div><span>Política precio</span><strong>{etiquetaPolitica(producto.politicaPrecio)}</strong></div>
            {producto.margenObjetivoPct != null ? (
              <div><span>Margen objetivo</span><strong>{producto.margenObjetivoPct}%</strong></div>
            ) : null}
            {producto.precioVentaSugerido != null ? (
              <div><span>Precio sugerido</span><strong>{dinero(producto.precioVentaSugerido)}</strong></div>
            ) : null}
            <div><span>Margen</span><strong>{dinero(margen)}</strong></div>
            <div><span>Alcohólico</span><strong>{producto.esAlcoholico ? 'Sí' : 'No'}</strong></div>
            <div><span>Vence</span><strong>{producto.fechaVencimiento || '—'}</strong></div>
          </>
        )}
      </div>

      {presentaciones.length ? (
        <>
          <h3>{soloCatalogo ? 'Presentaciones disponibles' : 'Presentaciones'}</h3>
          <ul className="receipt-lines fac-lines prod-presentaciones">
            {presentaciones.map((presentacion) => (
              <li key={presentacion.id || presentacion.nombre}>
                <span>{presentacion.nombre} (×{presentacion.factorAUnidadMinima})</span>
                {!soloCatalogo ? (
                  <span className="prod-presentacion-estado">
                    {presentacion.activo ? 'Activa' : 'Inactiva'}
                    {presentacion.id && puedeGestionar ? (
                      <button
                        type="button"
                        className="btn link no-print"
                        onClick={() => onEditarPresentacion?.(presentacion)}
                      >
                        Editar
                      </button>
                    ) : null}
                  </span>
                ) : null}
              </li>
            ))}
          </ul>
        </>
      ) : null}

      {!soloCatalogo && puedeGestionar ? (
        <footer className="fac-detail-actions no-print">
          <button type="button" className="btn secondary" onClick={() => onEditar?.(producto)}>Editar</button>
          <button type="button" className="btn secondary" onClick={() => onPresentacion?.(producto)}>+ Presentación</button>
          {esAdmin && producto.eliminable ? (
            <button type="button" className="btn danger" onClick={() => onEliminar?.(producto)}>Eliminar</button>
          ) : null}
          {esAdmin && !producto.eliminable ? (
            <p className="pay-note warn">{producto.motivoNoEliminable}</p>
          ) : null}
        </footer>
      ) : null}
    </article>
  );
}
