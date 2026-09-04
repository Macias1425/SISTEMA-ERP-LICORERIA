import { useEffect, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import CatalogToolbar, { CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import Button from '../../components/ui/Button';
import { reporteService } from '../../services/reporteService';
import { descargarCsv, archivoDocumento, textoCelda } from '../../utils/exportCsv';
import Pagination from '../../components/ui/Pagination';
import ReportePrintHeader from '../../components/reportes/ReportePrintHeader';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

const FILTROS = [
  ['atencion', 'Atención'],
  ['VENCIDO', 'Vencidos'],
  ['POR_VENCER', 'Por vencer'],
  ['SIN_FECHA', 'Sin fecha'],
  ['VIGENTE', 'Vigentes'],
  ['todos', 'Todos'],
];

function fechaCorta(valor) {
  if (!valor) return '—';
  return new Date(`${valor}T00:00:00`).toLocaleDateString('es-NI');
}

function chipEstado(estado) {
  if (estado === 'VENCIDO') return 'cat-badge cat-badge-warn';
  if (estado === 'POR_VENCER') return 'cat-badge cat-badge-info';
  if (estado === 'SIN_FECHA') return 'cat-badge';
  return 'cat-badge cat-badge-ok';
}

function etiquetaFiltro(filtro) {
  if (filtro === 'atencion') return 'Atención (vencidos y por vencer)';
  return FILTROS.find(([valor]) => valor === filtro)?.[1] || 'Todos los estados';
}

function etiquetaEstado(estado) {
  if (estado === 'VENCIDO') return 'Vencido';
  if (estado === 'POR_VENCER') return 'Por vencer';
  if (estado === 'SIN_FECHA') return 'Sin fecha';
  return 'Vigente';
}

function textoDias(dias) {
  if (dias == null) return '—';
  const n = Number(dias);
  if (n < 0) return `Venció hace ${Math.abs(n)} día(s)`;
  if (n === 0) return 'Vence hoy';
  return `${n} día(s)`;
}

export default function VencimientosPanel() {
  const [items, setItems] = useState([]);
  const [dias, setDias] = useState(30);
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState('atencion');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  async function cargar(diasAlerta = dias, paginaDestino = 0, extras = {}) {
    setCargando(true);
    setError('');
    try {
      const respuesta = await reporteService.vencimientos(Number(diasAlerta) || 30, {
        busqueda: extras.busqueda ?? busqueda,
        estado: extras.filtro ?? filtro,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setItems(contenidoPagina(respuesta));
      setPaginaMeta(metaPagina(respuesta));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar(30);
  }, []);

  const visibles = items;

  const vencidos = items.filter((item) => item.estado === 'VENCIDO').length;
  const porVencer = items.filter((item) => item.estado === 'POR_VENCER').length;
  const sinFecha = items.filter((item) => item.estado === 'SIN_FECHA').length;

  function exportarCsv() {
    descargarCsv(archivoDocumento('Alertas de vencimiento'), [
      { key: 'codigo', label: 'Código de producto', format: (f) => textoCelda(f.codigo) },
      { key: 'nombre', label: 'Producto', format: (f) => textoCelda(f.nombre) },
      { key: 'stockActual', label: 'Existencia actual' },
      { key: 'unidadMinima', label: 'Unidad mínima', format: (f) => textoCelda(f.unidadMinima, 'UMM') },
      { key: 'fechaVencimiento', label: 'Fecha de vencimiento', format: (f) => fechaCorta(f.fechaVencimiento) },
      { key: 'diasRestantes', label: 'Plazo restante', format: (f) => textoDias(f.diasRestantes) },
      { key: 'estado', label: 'Estado', format: (f) => etiquetaEstado(f.estado) },
    ], visibles, {
      titulo: 'Alertas de vencimiento de inventario',
      subtitulo: `Productos clasificados con un umbral de ${dias} día(s)`,
      filtros: etiquetaFiltro(filtro),
    });
  }

  return (
    <div className="dash-stack rep-print-area">
      <ReportePrintHeader
        tipo="Reporte de inventario"
        titulo="Alertas de vencimiento"
        subtitulo={`Clasificación con umbral de ${dias} día(s). Los vencidos no deben venderse.`}
        registros={paginaMeta.totalElementos}
        filtros={etiquetaFiltro(filtro)}
      />
      {error ? <p className="auth-error" role="alert">{error}</p> : null}

      <div className="cat-kpi-grid no-print">
        <article className="cat-kpi">
          <span>Vencidos</span>
          <strong className={vencidos ? 'kpi-neg' : ''}>{vencidos}</strong>
          <small>Ya no deberían venderse</small>
        </article>
        <article className="cat-kpi">
          <span>Por vencer</span>
          <strong>{porVencer}</strong>
          <small>Dentro de {dias} día(s)</small>
        </article>
        <article className="cat-kpi">
          <span>Sin fecha</span>
          <strong>{sinFecha}</strong>
          <small>Con stock sin vencimiento cargado</small>
        </article>
      </div>

      <article className="card">
        <h3>Alertas de vencimiento</h3>
        <form
          className="form-actions no-print"
          style={{ marginBottom: '0.85rem', alignItems: 'end', flexWrap: 'wrap', gap: '0.75rem' }}
          onSubmit={(event) => {
            event.preventDefault();
            cargar(dias);
          }}
        >
          <label>
            Días de alerta
            <input
              className="search-input"
              type="number"
              min="1"
              max="365"
              value={dias}
              onChange={(event) => setDias(Number(event.target.value))}
              style={{ width: '8rem' }}
            />
          </label>
          <Button type="submit" disabled={cargando}>Actualizar</Button>
          <Button type="button" variant="secondary" onClick={() => window.print()}>Imprimir</Button>
          <Button type="button" variant="secondary" disabled={!visibles.length} onClick={exportarCsv}>
            Exportar CSV
          </Button>
        </form>

        <div className="no-print" style={{ marginBottom: '1rem' }}>
          <CatalogTabs
            tabs={FILTROS}
            active={filtro}
            onChange={(valor) => { setFiltro(valor); cargar(dias, 0, { filtro: valor }); }}
          />
        </div>

        <div className="no-print" style={{ marginBottom: '1rem' }}>
          <CatalogToolbar
            searchValue={busqueda}
            onSearchChange={setBusqueda}
            onSubmit={() => cargar(dias, 0)}
            searchPlaceholder="Buscar por código o producto…"
            activeCount={busqueda.trim() ? 1 : 0}
            onClearFilters={() => { setBusqueda(''); cargar(dias, 0, { busqueda: '' }); }}
          />
        </div>

        {cargando ? <p className="placeholder">Cargando…</p> : null}
        {!cargando && !visibles.length ? (
          <p className="placeholder">
            {filtro === 'atencion'
              ? 'No hay productos vencidos ni próximos a vencer.'
              : 'No hay productos en este filtro.'}
          </p>
        ) : null}
        {!cargando && visibles.length ? (
          <table className="data-table">
            <thead>
              <tr>
                <th>Código</th>
                <th>Producto</th>
                <th>Stock</th>
                <th>Vence</th>
                <th>Días</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((item) => (
                <tr key={item.productoId || item.codigo}>
                  <td>{item.codigo}</td>
                  <td>{item.nombre}</td>
                  <td>{item.stockActual} {item.unidadMinima || 'UMM'}</td>
                  <td>{fechaCorta(item.fechaVencimiento)}</td>
                  <td>{textoDias(item.diasRestantes)}</td>
                  <td>
                    <span className={chipEstado(item.estado)}>{etiquetaEstado(item.estado)}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
        <Pagination
          pagina={pagina}
          totalPaginas={paginaMeta.totalPaginas}
          totalElementos={paginaMeta.totalElementos}
          tamano={paginaMeta.tamano}
          cargando={cargando}
          onChange={(nueva) => cargar(dias, nueva)}
        />
      </article>
    </div>
  );
}
