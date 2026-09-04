import EmptyState from '../ui/EmptyState';
import Pagination from '../ui/Pagination';
import { dinero } from '../../utils/formato';

export default function ProveedorDetalle({
  proveedor,
  precios,
  paginaMeta,
  pagina = 0,
  onPagina,
  onEditar,
  onPrecio,
}) {
  if (!proveedor) {
    return (
      <EmptyState
        className="fac-detail-empty"
        icon="truck"
        title="Seleccione un proveedor"
        message="Administre contacto y catálogo de precios que alimentan las compras."
      />
    );
  }

  return (
    <article className="fac-detail">
      <header className="fac-detail-head">
        <div>
          <p className="brand-kicker">Proveedor</p>
          <h2>{proveedor.nombre}</h2>
          <p className="fac-detail-meta">{proveedor.documento || 'Sin RUC'}</p>
        </div>
        <span className={`fac-estado${proveedor.activo ? ' fac-estado-ok' : ''}`}>
          {proveedor.activo ? 'Activo' : 'Inactivo'}
        </span>
      </header>

      <div className="fac-detail-grid">
        <div><span>Contacto</span><strong>{proveedor.contactoNombre || '—'}</strong></div>
        <div><span>Teléfono</span><strong>{proveedor.telefono || '—'}</strong></div>
        <div><span>Correo</span><strong>{proveedor.email || '—'}</strong></div>
        <div>
          <span>Precios en catálogo</span>
          <strong>{proveedor.cantidadPrecios ?? paginaMeta?.totalElementos ?? precios?.length ?? 0}</strong>
        </div>
      </div>

      <h3>Catálogo de precios</h3>
      {!precios?.length ? (
        <p className="placeholder">Sin precios registrados. Agregue productos para usar en compras.</p>
      ) : (
        <>
          <ul className="receipt-lines fac-lines">
            {precios.map((precio) => (
              <li key={precio.id || `${precio.productoId}-${precio.presentacionId}`}>
                <span>
                  {precio.productoCodigo} · {precio.productoNombre}
                  {precio.presentacionNombre ? ` (${precio.presentacionNombre})` : ''}
                </span>
                <span>{dinero(precio.precioUnitario)}</span>
              </li>
            ))}
          </ul>
          {paginaMeta ? (
            <Pagination
              pagina={pagina}
              totalPaginas={paginaMeta.totalPaginas}
              totalElementos={paginaMeta.totalElementos}
              tamano={paginaMeta.tamano}
              onChange={onPagina}
            />
          ) : null}
        </>
      )}

      <footer className="fac-detail-actions no-print">
        <button type="button" className="btn secondary" onClick={() => onEditar?.(proveedor)}>Editar datos</button>
        <button type="button" className="btn primary" onClick={() => onPrecio?.(proveedor)}>Agregar precio</button>
      </footer>
    </article>
  );
}
