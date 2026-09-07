import { useEffect, useState } from 'react';
import Button from '../../components/ui/Button';
import { mensajeError } from '../../auth/AuthContext';
import { clienteService } from '../../services/clienteService';
import { dinero } from '../../utils/formato';
import { contenidoPagina, listarTodos } from '../../utils/paginaUtil';

export default function CreditosPage() {
  const [deudores, setDeudores] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [clienteLimiteId, setClienteLimiteId] = useState('');
  const [nuevoLimite, setNuevoLimite] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [abonos, setAbonos] = useState({});
  const [guardando, setGuardando] = useState(null);

  async function cargar() {
    setCargando(true);
    setError('');
    try {
      const [listaDeudores, listaClientes] = await Promise.all([
        clienteService.deudores(),
        listarTodos((p) => clienteService.listar({ activo: true, ...p })).catch(() => []),
      ]);
      setDeudores(Array.isArray(listaDeudores) ? listaDeudores : []);
      const clientesArr = Array.isArray(listaClientes) ? listaClientes : contenidoPagina(listaClientes);
      setClientes(clientesArr.filter((c) => c.nombre !== 'Consumidor final'));
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  async function guardarLimite(event) {
    event.preventDefault();
    if (!clienteLimiteId) {
      setError('Seleccione un cliente');
      return;
    }
    const cliente = clientes.find((c) => String(c.id) === String(clienteLimiteId));
    if (!cliente) return;
    setGuardando('limite');
    setError('');
    setOk('');
    try {
      await clienteService.actualizar(cliente.id, {
        ...cliente,
        limiteCredito: Number(nuevoLimite || 0),
      });
      setOk(`Límite actualizado para ${cliente.nombre}`);
      setNuevoLimite('');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(null);
    }
  }

  async function abonar(cliente) {
    const monto = Number(abonos[cliente.id] || 0);
    if (!(monto > 0)) {
      setError('Indique un monto de abono válido');
      return;
    }
    setGuardando(cliente.id);
    setError('');
    setOk('');
    try {
      await clienteService.abonar(cliente.id, monto);
      setOk(`Abono de ${dinero(monto)} registrado para ${cliente.nombre}`);
      setAbonos((prev) => ({ ...prev, [cliente.id]: '' }));
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(null);
    }
  }

  return (
    <section className="page-shell">
      <header className="page-header">
        <div>
          <h1>Crédito a clientes</h1>
          <p>Asigne límite de fiado, cobre a crédito en el POS y registre abonos aquí.</p>
        </div>
        <Button type="button" variant="secondary" onClick={cargar} disabled={cargando}>
          Actualizar
        </Button>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <form className="corte-card" onSubmit={guardarLimite} style={{ marginBottom: '1.25rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1rem' }}>Asignar / cambiar límite</h2>
        <div className="corte-actions">
          <label className="corte-fecha">
            <span>Cliente</span>
            <select value={clienteLimiteId} onChange={(e) => setClienteLimiteId(e.target.value)} required>
              <option value="">Seleccione…</option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre} · límite {dinero(c.limiteCredito)} · saldo {dinero(c.saldoCredito)}
                </option>
              ))}
            </select>
          </label>
          <label className="corte-fecha">
            <span>Nuevo límite (C$)</span>
            <input
              type="number"
              min="0"
              step="0.01"
              value={nuevoLimite}
              onChange={(e) => setNuevoLimite(e.target.value)}
              required
            />
          </label>
          <Button type="submit" disabled={guardando === 'limite'}>
            Guardar límite
          </Button>
        </div>
        <p className="muted" style={{ marginBottom: 0 }}>
          Requiere permiso de gestión de clientes (admin). Límite 0 = no puede fiado.
        </p>
      </form>

      {cargando ? <p className="placeholder">Cargando deudores…</p> : null}

      {!cargando && !deudores.length ? (
        <p className="placeholder">No hay clientes con saldo pendiente.</p>
      ) : null}

      {deudores.length ? (
        <table className="data-table">
          <thead>
            <tr>
              <th>Cliente</th>
              <th>Límite</th>
              <th>Saldo</th>
              <th>Disponible</th>
              <th>Abono</th>
            </tr>
          </thead>
          <tbody>
            {deudores.map((c) => {
              const disponible = Math.max(0, Number(c.limiteCredito || 0) - Number(c.saldoCredito || 0));
              return (
                <tr key={c.id}>
                  <td>
                    <strong>{c.nombre}</strong>
                    <div className="muted">{c.telefono || c.ruc || '—'}</div>
                  </td>
                  <td>{dinero(c.limiteCredito)}</td>
                  <td>{dinero(c.saldoCredito)}</td>
                  <td>{dinero(disponible)}</td>
                  <td>
                    <div className="corte-actions">
                      <input
                        type="number"
                        min="0.01"
                        step="0.01"
                        placeholder="0.00"
                        value={abonos[c.id] ?? ''}
                        onChange={(e) => setAbonos((prev) => ({ ...prev, [c.id]: e.target.value }))}
                        style={{ width: '7rem' }}
                      />
                      <Button
                        type="button"
                        disabled={guardando === c.id}
                        onClick={() => abonar(c)}
                      >
                        {guardando === c.id ? '…' : 'Abonar'}
                      </Button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      ) : null}
    </section>
  );
}
