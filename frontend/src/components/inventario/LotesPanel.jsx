import { useCallback, useEffect, useMemo, useState } from 'react';

import { mensajeError } from '../../auth/AuthContext';
import { inventarioService } from '../../services/inventarioService';
import { fecha, fechaHora, moneda } from '../../utils/formato';
import Pagination from '../ui/Pagination';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';
import LoteNivelChip from './LoteNivelChip';

const VENTANAS_DIAS = [15, 30, 60, 90];

function textoDias(dias) {
  if (dias === null || dias === undefined) return 'Sin vencimiento';
  if (dias < 0) return `Vencido hace ${Math.abs(dias)} d`;
  if (dias === 0) return 'Vence hoy';
  return `${dias} d`;
}

/**
 * Trazabilidad de existencias por lote. En licorería el costo y el vencimiento
 * cambian en cada compra, así que el saldo global no basta: hay que saber
 * qué botella sale primero (FEFO) y cuánto capital está por caducar.
 */
export default function LotesPanel({ productos }) {
  const [modo, setModo] = useState('por-vencer');
  const [productoId, setProductoId] = useState('');
  const [dias, setDias] = useState(30);
  const [lotes, setLotes] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  const cargar = useCallback(async (paginaDestino = 0) => {
    setCargando(true);
    setError('');
    try {
      const datos = modo === 'producto' && productoId
        ? await inventarioService.lotesFefo(productoId, { pagina: paginaDestino, tamano: TAMANO_PAGINA_DEFAULT })
        : await inventarioService.lotesPorVencer(dias, { pagina: paginaDestino, tamano: TAMANO_PAGINA_DEFAULT });
      setLotes(contenidoPagina(datos));
      setPaginaMeta(metaPagina(datos));
      setPagina(paginaDestino);
    } catch (err) {
      setLotes([]);
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }, [modo, productoId, dias]);

  useEffect(() => {
    if (modo === 'producto' && !productoId) {
      setLotes([]);
      return;
    }
    cargar();
  }, [cargar, modo, productoId]);

  const resumen = useMemo(() => {
    const vivos = lotes.filter((lote) => Number(lote.cantidadDisponibleUmm || 0) > 0);
    return {
      lotes: vivos.length,
      unidades: vivos.reduce((suma, lote) => suma + Number(lote.cantidadDisponibleUmm || 0), 0),
      vencidos: vivos.filter((lote) => lote.nivelVencimiento === 'VENCIDO').length,
      porVencer: vivos.filter((lote) => lote.nivelVencimiento === 'POR_VENCER').length,
      valor: vivos.reduce((suma, lote) => suma + Number(lote.valorInventario || 0), 0),
    };
  }, [lotes]);

  return (
    <>
      <div className="fac-stats inv-stats">
        <article className="pos-stat accent">
          <span>Lotes con saldo</span>
          <strong>{resumen.lotes}</strong>
          <small>{resumen.unidades} UMM disponibles</small>
        </article>
        <article className="pos-stat warn">
          <span>Por vencer</span>
          <strong>{resumen.porVencer}</strong>
          <small>Priorice su salida o promoción</small>
        </article>
        <article className="pos-stat">
          <span>Vencidos en bodega</span>
          <strong>{resumen.vencidos}</strong>
          <small>Deben pasar a merma</small>
        </article>
        <article className="pos-stat">
          <span>Capital en estos lotes</span>
          <strong>{moneda(resumen.valor)}</strong>
          <small>Valorado al costo de compra</small>
        </article>
      </div>

      <form
        className="fac-filters inv-filters"
        onSubmit={(evento) => {
          evento.preventDefault();
          cargar();
        }}
      >
        <label>
          Vista
          <select
            value={modo}
            onChange={(evento) => {
              setModo(evento.target.value);
              setError('');
            }}
          >
            <option value="por-vencer">Próximos a vencer</option>
            <option value="producto">Rotación FEFO por producto</option>
          </select>
        </label>

        {modo === 'producto' ? (
          <label>
            Producto
            <select value={productoId} onChange={(evento) => setProductoId(evento.target.value)}>
              <option value="">Seleccione…</option>
              {productos.map((producto) => (
                <option key={producto.id} value={producto.id}>
                  {producto.codigo} · {producto.nombre}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <label>
            Ventana
            <select value={dias} onChange={(evento) => setDias(Number(evento.target.value))}>
              {VENTANAS_DIAS.map((valor) => (
                <option key={valor} value={valor}>Próximos {valor} días</option>
              ))}
            </select>
          </label>
        )}

        <button type="submit" className="btn secondary" disabled={cargando}>
          {cargando ? 'Consultando…' : 'Actualizar'}
        </button>
      </form>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}

      <article className="card inv-card">
        <div className="inv-card-head">
          <h3>{modo === 'producto' ? 'Orden sugerido de salida (FEFO)' : 'Lotes próximos a vencer'}</h3>
          <span className="hint">
            {modo === 'producto'
              ? 'El primer lote de la lista es el que debe entregarse'
              : `Vencimientos dentro de ${dias} días`}
          </span>
        </div>

        {cargando ? (
          <p className="placeholder">Cargando lotes…</p>
        ) : modo === 'producto' && !productoId ? (
          <p className="placeholder">Seleccione un producto para ver su rotación.</p>
        ) : !lotes.length ? (
          <p className="placeholder">
            {modo === 'producto'
              ? 'Este producto no tiene lotes con saldo. Los lotes se crean al recibir compras.'
              : 'Ningún lote vence en la ventana seleccionada.'}
          </p>
        ) : (
          <div className="fac-table-wrap">
            <table className="data-table fac-table inv-table">
              <thead>
                <tr>
                  {modo === 'producto' ? <th>Salida</th> : null}
                  <th>Lote</th>
                  <th>Producto</th>
                  <th>Disponible</th>
                  <th>Ingreso</th>
                  <th>Vence</th>
                  <th>Estado</th>
                  <th>Costo UMM</th>
                  <th>Valor</th>
                  <th>Origen</th>
                </tr>
              </thead>
              <tbody>
                {lotes.map((lote, indice) => (
                  <tr key={lote.id} className={lote.nivelVencimiento === 'VENCIDO' ? 'inv-row-critico' : undefined}>
                    {modo === 'producto' ? <td className="inv-orden-fefo">{pagina * paginaMeta.tamano + indice + 1}º</td> : null}
                    <td><code>{lote.codigo}</code></td>
                    <td>
                      <strong>{lote.productoNombre || `#${lote.productoId}`}</strong>
                      {lote.productoCodigo ? <small className="hint"> {lote.productoCodigo}</small> : null}
                    </td>
                    <td>{lote.cantidadDisponibleUmm} / {lote.cantidadInicialUmm}</td>
                    <td>{fechaHora(lote.fechaIngreso)}</td>
                    <td>{fecha(lote.fechaVencimiento)}</td>
                    <td>
                      <LoteNivelChip nivel={lote.nivelVencimiento} />
                      <small className="hint"> {textoDias(lote.diasParaVencer)}</small>
                    </td>
                    <td>{moneda(lote.costoUnitarioUmm)}</td>
                    <td>{moneda(lote.valorInventario)}</td>
                    <td>
                      {lote.compraId ? `Compra #${lote.compraId}` : 'Ajuste manual'}
                      {lote.proveedorNombre ? <small className="hint"> · {lote.proveedorNombre}</small> : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination
          pagina={pagina}
          totalPaginas={paginaMeta.totalPaginas}
          totalElementos={paginaMeta.totalElementos}
          tamano={paginaMeta.tamano}
          cargando={cargando}
          onChange={(nueva) => cargar(nueva)}
        />
      </article>
    </>
  );
}
