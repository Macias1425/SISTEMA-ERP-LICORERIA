import { useCallback, useEffect, useState } from 'react';

import { mensajeError } from '../../auth/AuthContext';
import { finanzasService } from '../../services/finanzasService';
import { descargarCsv, archivoDocumento, textoCelda, cordoba } from '../../utils/exportCsv';
import { moneda, porcentaje } from '../../utils/formato';
import Pagination from '../ui/Pagination';
import { TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

const NIVELES = {
  PERDIDA: { texto: 'Pérdida', clase: 'fin-riesgo-perdida' },
  CRITICO: { texto: 'Crítico', clase: 'fin-riesgo-critico' },
  BAJO: { texto: 'Bajo', clase: 'fin-riesgo-bajo' },
  SANO: { texto: 'Sano', clase: 'fin-riesgo-sano' },
};

function NivelChip({ nivel }) {
  const { texto, clase } = NIVELES[nivel] || NIVELES.CRITICO;
  return <span className={`fin-riesgo-chip ${clase}`}>{texto}</span>;
}

/**
 * Compara el costo real de las existencias contra el precio de venta vigente.
 * Es el aviso que evita seguir vendiendo a pérdida cuando el proveedor sube precios.
 */
export default function MargenRiesgoPanel() {
  const [objetivo, setObjetivo] = useState(20);
  const [soloRiesgo, setSoloRiesgo] = useState(true);
  const [resumen, setResumen] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [pagina, setPagina] = useState(0);

  const cargar = useCallback(async (params = {}, paginaDestino = 0) => {
    setCargando(true);
    setError('');
    try {
      const datos = await finanzasService.margenRiesgo({
        objetivo: params.objetivo ?? objetivo,
        soloRiesgo: params.soloRiesgo ?? soloRiesgo,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setResumen(datos);
      setPagina(paginaDestino);
    } catch (err) {
      setResumen(null);
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }, [objetivo, soloRiesgo]);

  useEffect(() => {
    cargar();
    // La carga inicial usa los valores por defecto; los cambios se aplican al enviar el filtro.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const items = resumen?.items || [];

  function exportar() {
    descargarCsv(archivoDocumento('Productos con margen en riesgo'), [
      { key: 'codigo', label: 'Código de producto', format: (f) => textoCelda(f.codigo) },
      { key: 'nombre', label: 'Producto', format: (f) => textoCelda(f.nombre) },
      { key: 'stockActual', label: 'Existencia (UMM)' },
      { key: 'costoUmm', label: 'Costo de catálogo (C$)', format: (f) => cordoba(f.costoUmm) },
      { key: 'costoLotesUmm', label: 'Costo promedio de lotes (C$)', format: (f) => cordoba(f.costoLotesUmm) },
      { key: 'precioDetalUmm', label: 'Precio de venta al detal (C$)', format: (f) => cordoba(f.precioDetalUmm) },
      { key: 'margenDetalPorcentaje', label: 'Margen al detal (%)', format: (f) => porcentaje(f.margenDetalPorcentaje) },
      { key: 'nivel', label: 'Nivel de riesgo', format: (f) => (NIVELES[f.nivel]?.texto || textoCelda(f.nivel)) },
      { key: 'precioSugeridoUmm', label: 'Precio sugerido (C$)', format: (f) => cordoba(f.precioSugeridoUmm) },
    ], items, {
      titulo: 'Productos con margen en riesgo',
      subtitulo: `Comparación de costo real contra precio de venta. Objetivo de margen: ${objetivo} %.`,
      filtros: soloRiesgo ? 'Solo productos en riesgo' : 'Todos los productos evaluados',
    });
  }

  return (
    <>
      <form
        className="fac-filters"
        onSubmit={(evento) => {
          evento.preventDefault();
          cargar();
        }}
      >
        <label>
          Margen objetivo (%)
          <input
            type="number"
            min="1"
            max="90"
            step="0.5"
            value={objetivo}
            onChange={(evento) => setObjetivo(Number(evento.target.value))}
          />
        </label>
        <label className="check">
          <input
            type="checkbox"
            checked={soloRiesgo}
            onChange={(evento) => {
              const valor = evento.target.checked;
              setSoloRiesgo(valor);
              cargar({ soloRiesgo: valor });
            }}
          />
          Mostrar solo productos en riesgo
        </label>
        <button type="submit" className="btn secondary" disabled={cargando}>
          {cargando ? 'Analizando…' : 'Analizar'}
        </button>
        <button type="button" className="btn secondary" onClick={exportar} disabled={!items.length}>
          Exportar CSV
        </button>
      </form>

      {error ? <p className="auth-error" role="alert">{error}</p> : null}

      {resumen ? (
        <div className="cat-kpi-grid fin-kpi-grid">
          <article className="cat-kpi">
            <span>Vendiendo a pérdida</span>
            <strong className={resumen.productosEnPerdida > 0 ? 'kpi-neg' : ''}>{resumen.productosEnPerdida}</strong>
            <small>Precio por debajo del costo</small>
          </article>
          <article className="cat-kpi">
            <span>Margen crítico</span>
            <strong>{resumen.productosCriticos}</strong>
            <small>Menos de 5% sobre la venta</small>
          </article>
          <article className="cat-kpi">
            <span>Bajo el objetivo</span>
            <strong>{resumen.productosBajos}</strong>
            <small>Meta: {porcentaje(resumen.margenObjetivoPorcentaje, 0)}</small>
          </article>
          <article className="cat-kpi">
            <span>Capital en riesgo</span>
            <strong>{moneda(resumen.capitalEnRiesgo)}</strong>
            <small>Costo inmovilizado sin margen</small>
          </article>
          <article className="cat-kpi">
            <span>Margen promedio</span>
            <strong>{porcentaje(resumen.margenPromedioPonderado)}</strong>
            <small>Ponderado por existencias</small>
          </article>
        </div>
      ) : null}

      <article className="card fin-table-card">
        <header className="aud-table-header">
          <h3>Productos por revisar</h3>
          <span>
            {resumen
              ? `${items.length} de ${resumen.productosEvaluados} producto(s) activos · inventario valorado en ${moneda(resumen.valorInventarioCosto)}`
              : 'Sin datos'}
          </span>
        </header>

        {cargando ? (
          <p className="placeholder">Calculando márgenes…</p>
        ) : !items.length ? (
          <p className="placeholder">
            {soloRiesgo
              ? 'Ningún producto está bajo el margen objetivo. Buen trabajo.'
              : 'No hay productos activos para evaluar.'}
          </p>
        ) : (
          <div className="aud-table-wrap">
            <table className="data-table aud-table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Producto</th>
                  <th>Stock</th>
                  <th>Costo catálogo</th>
                  <th>Costo lotes</th>
                  <th>Precio detal</th>
                  <th>Margen</th>
                  <th>Nivel</th>
                  <th>Sugerido</th>
                  <th>Observación</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.productoId}>
                    <td><code>{item.codigo}</code></td>
                    <td>
                      <strong>{item.nombre}</strong>
                      {item.marca ? <small className="hint"> {item.marca}</small> : null}
                    </td>
                    <td>{item.stockActual}</td>
                    <td>{moneda(item.costoUmm)}</td>
                    <td className={item.costoLotesUmm && item.costoUmm
                      && Number(item.costoLotesUmm) !== Number(item.costoUmm) ? 'fin-costo-desfasado' : undefined}>
                      {moneda(item.costoLotesUmm)}
                    </td>
                    <td>{moneda(item.precioDetalUmm)}</td>
                    <td>{porcentaje(item.margenDetalPorcentaje)}</td>
                    <td><NivelChip nivel={item.nivel} /></td>
                    <td>{moneda(item.precioSugeridoUmm)}</td>
                    <td className="fin-riesgo-motivo">{item.motivo || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination
          pagina={pagina}
          totalPaginas={resumen?.totalPaginas || 1}
          totalElementos={resumen?.totalElementos || items.length}
          tamano={resumen?.tamano || TAMANO_PAGINA_DEFAULT}
          cargando={cargando}
          onChange={(nueva) => cargar({}, nueva)}
        />
      </article>
    </>
  );
}
