import { useEffect, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import { inventarioService } from '../../services/inventarioService';
import { contenidoPagina, metaPagina } from '../../utils/paginaUtil';
import Modal from '../ui/Modal';
import Button from '../ui/Button';
import Pagination from '../ui/Pagination';

function fechaCorta(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

export default function ProductoKardexModal({ open, producto, onClose }) {
  const [movimientos, setMovimientos] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: 30,
  });

  async function cargar(paginaDestino = 0) {
    if (!producto?.id) return;
    setCargando(true);
    setError('');
    try {
      const resp = await inventarioService.listarMovimientos({
        productoId: producto.id,
        pagina: paginaDestino,
        tamano: 30,
      });
      setMovimientos(contenidoPagina(resp));
      setPaginaMeta(metaPagina(resp));
      setPagina(paginaDestino);
    } catch (err) {
      setMovimientos([]);
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    if (!open || !producto?.id) return;
    cargar(0);
  }, [open, producto?.id]);

  return (
    <Modal
      open={open && Boolean(producto)}
      title={producto?.nombre || 'Kardex'}
      subtitle={`${producto?.codigo || ''} · Stock ${producto?.stockActual ?? 0} UMM`}
      onClose={onClose}
      size="lg"
      footer={(
        <Button variant="secondary" type="button" onClick={onClose}>Cerrar</Button>
      )}
    >
      {error ? <p className="pos-alert">{error}</p> : null}
      {cargando ? (
        <p className="placeholder">Cargando movimientos…</p>
      ) : !movimientos.length ? (
        <p className="placeholder">Sin movimientos registrados para este producto.</p>
      ) : (
        <>
          <div className="fac-table-wrap">
            <table className="data-table fac-table inv-kardex-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Cant.</th>
                  <th>UMM</th>
                  <th>Stock</th>
                  <th>Motivo / ref.</th>
                </tr>
              </thead>
              <tbody>
                {movimientos.map((mov) => (
                  <tr key={mov.id}>
                    <td>{fechaCorta(mov.fecha)}</td>
                    <td>{mov.tipo}</td>
                    <td>{mov.cantidadPresentacion}</td>
                    <td>{mov.cantidadUmm}</td>
                    <td>{mov.stockResultante}</td>
                    <td>{mov.motivo || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            pagina={pagina}
            totalPaginas={paginaMeta.totalPaginas}
            totalElementos={paginaMeta.totalElementos}
            tamano={paginaMeta.tamano}
            cargando={cargando}
            onChange={cargar}
          />
        </>
      )}
    </Modal>
  );
}
