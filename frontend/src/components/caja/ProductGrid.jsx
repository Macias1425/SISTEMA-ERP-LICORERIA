import Icon from '../ui/Icon';
import EmptyState from '../ui/EmptyState';
import { dinero } from '../../utils/formato';

function hoyLocal() {
  const fecha = new Date();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

function presentacionVenta(producto) {
  const activas = (producto.presentaciones || []).filter((item) => item.activo !== false);
  return activas.find((item) => item.factorAUnidadMinima === 1) || activas[0] || null;
}

export default function ProductGrid({ productos, licorPermitido, reservadoUmm = {}, onAgregar }) {
  const hoy = hoyLocal();

  if (!productos.length) {
    return (
      <EmptyState
        className="pos-product-empty"
        icon="package"
        title="No hay productos"
        message="Pruebe otra búsqueda o categoría."
        compact
      />
    );
  }

  return (
    <section className="pos-product-grid" aria-label="Catálogo de productos">
      {productos.map((producto) => {
        const presentacion = presentacionVenta(producto);
        const factor = presentacion?.factorAUnidadMinima || 1;
        const disponible = (producto.stockActual ?? 0) - (reservadoUmm[producto.id] || 0);
        const vencido = Boolean(producto.fechaVencimiento && producto.fechaVencimiento < hoy);
        const bloqueado = Boolean(producto.esAlcoholico) && !licorPermitido;
        const sinStock = disponible < factor;
        const deshabilitado = bloqueado || sinStock || vencido || !presentacion;

        let motivo = '';
        if (vencido) motivo = 'Vencido';
        else if (bloqueado) motivo = 'Horario cerrado';
        else if (sinStock) motivo = 'Agotado';

        return (
          <button
            key={producto.id}
            type="button"
            className={`pos-product-card${deshabilitado ? ' is-disabled' : ''}`}
            disabled={deshabilitado}
            onClick={() => onAgregar(producto)}
            aria-label={`Agregar ${producto.nombre}, ${dinero(producto.precioVenta)}`}
          >
            <div className="pos-product-card-top">
              {producto.esAlcoholico ? (
                <span className="pos-product-tag pos-product-tag-alcohol">
                  <Icon name="wine" size={14} strokeWidth={2.25} />
                  Licor
                </span>
              ) : (
                <span className="pos-product-tag">Producto</span>
              )}
              {!deshabilitado ? (
                <span className="pos-product-add" aria-hidden>
                  <Icon name="plus" size={16} strokeWidth={2.5} />
                </span>
              ) : (
                <span className="pos-product-blocked">{motivo}</span>
              )}
            </div>
            <strong className="pos-product-name">{producto.nombre}</strong>
            <span className="pos-product-code">{producto.codigo}</span>
            <div className="pos-product-foot">
              <span className="pos-product-price">{dinero(producto.precioVenta)}</span>
              <span className={`pos-product-stock${sinStock ? ' is-out' : disponible <= (producto.stockMinimo || 0) ? ' is-low' : ''}`}>
                {sinStock ? 'Sin stock' : `${disponible} disp.`}
              </span>
            </div>
          </button>
        );
      })}
    </section>
  );
}
