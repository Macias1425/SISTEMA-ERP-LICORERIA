import { dinero } from '../../utils/formato';
import { POLITICA_PRECIO } from '../../utils/politicaPrecio';

/** Solo muestra impacto para SUGERIDO y MANUAL. Automático actúa en silencio. */
export default function ImpactoRecepcionPanel({ impactos = [] }) {
  const visibles = (impactos || []).filter(
    (item) => item.decisionPrecio !== 'APLICADA_AUTO'
      && item.politicaPrecio !== POLITICA_PRECIO.AUTOMATICO_MARKUP,
  );
  if (!visibles.length) return null;

  return (
    <section className="com-impacto-panel" aria-label="Impacto de recepción">
      <h3>Resumen tras la recepción</h3>
      <div className="com-impacto-list">
        {visibles.map((item) => {
          const esSugerido = item.politicaPrecio === POLITICA_PRECIO.SUGERIDO
            || item.decisionPrecio === 'SOLO_SUGERIDO';
          return (
            <article
              key={item.productoId}
              className={`com-impacto-card${esSugerido ? ' sugerido' : ''}${item.alertaMargen ? ' warn' : ''}`}
            >
              <header>
                <strong>{item.productoNombre}</strong>
                <span className="com-impacto-politica">
                  {esSugerido ? 'Sugerido — revise precio' : 'Manual — solo costo'}
                </span>
              </header>
              {esSugerido ? (
                <>
                  <p className="com-impacto-msg com-impacto-sugerido-msg">
                    {item.mensajeDecision || (
                      <>
                        CPP actualizado a {dinero(item.costoPromedioNuevo)}/bot.
                        {' '}Para no perder margen, considere vender a{' '}
                        <strong>{dinero(item.ventaSugerida)}</strong>.
                        {' '}El POS sigue en {dinero(item.ventaAnterior)}.
                      </>
                    )}
                  </p>
                  <dl className="com-impacto-grid">
                    <div>
                      <dt>Costo ponderado</dt>
                      <dd>{dinero(item.costoPromedioAnterior)} → {dinero(item.costoPromedioNuevo)}</dd>
                    </div>
                    <div>
                      <dt>Precio sugerido</dt>
                      <dd>{dinero(item.ventaSugerida)}</dd>
                    </div>
                    <div>
                      <dt>Venta en POS (sin cambio)</dt>
                      <dd>{dinero(item.ventaAnterior)}</dd>
                    </div>
                  </dl>
                  <p className="field-hint">Registrado en Marcas y precios → Historial.</p>
                </>
              ) : (
                <dl className="com-impacto-grid">
                  <div>
                    <dt>Costo ponderado</dt>
                    <dd>{dinero(item.costoPromedioAnterior)} → {dinero(item.costoPromedioNuevo)}</dd>
                  </div>
                  <div>
                    <dt>Venta POS</dt>
                    <dd>{dinero(item.ventaAnterior)} (sin cambio)</dd>
                  </div>
                </dl>
              )}
            </article>
          );
        })}
      </div>
    </section>
  );
}
