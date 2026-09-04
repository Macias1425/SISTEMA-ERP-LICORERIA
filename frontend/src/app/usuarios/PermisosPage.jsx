import { useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import { CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import Button from '../../components/ui/Button';
import Icon from '../../components/ui/Icon';
import UsuarioRolChip from '../../components/usuarios/UsuarioRolChip';
import UsuariosSubNav from '../../components/usuarios/UsuariosSubNav';
import { permisoService } from '../../services/permisoService';
import { usuarioService } from '../../services/usuarioService';
import { contenidoPagina } from '../../utils/paginaUtil';

const TABS = [
  { id: 'roles', label: 'Permisos por rol' },
  { id: 'adicionales', label: 'Permisos adicionales' },
];

const ETIQUETA_ROL = {
  ADMIN: 'Administrador',
  CAJERO: 'Cajero',
  ALMACENISTA: 'Almacenista',
};

function agruparPorModulo(catalogo) {
  const mapa = new Map();
  catalogo.forEach((item) => {
    const lista = mapa.get(item.modulo) || [];
    lista.push(item);
    mapa.set(item.modulo, lista);
  });
  return [...mapa.entries()].sort(([a], [b]) => a.localeCompare(b));
}

export default function PermisosPage() {
  const [tab, setTab] = useState('roles');
  const [catalogo, setCatalogo] = useState([]);
  const [roles, setRoles] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [usuarioId, setUsuarioId] = useState('');
  const [permisosUsuario, setPermisosUsuario] = useState(null);
  const [seleccionados, setSeleccionados] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);

  async function cargarBase() {
    setCargando(true);
    setError('');
    try {
      const [cat, matriz, lista] = await Promise.all([
        permisoService.catalogo(),
        permisoService.roles(),
        usuarioService.listar({ activo: true, tamano: 200 }),
      ]);
      setCatalogo(cat);
      setRoles(matriz);
      setUsuarios(contenidoPagina(lista).filter((u) => u.rol !== 'ADMIN'));
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargarBase();
  }, []);

  useEffect(() => {
    if (!usuarioId) {
      setPermisosUsuario(null);
      setSeleccionados([]);
      return;
    }
    let cancelado = false;
    (async () => {
      setError('');
      try {
        const info = await permisoService.usuario(usuarioId);
        if (!cancelado) {
          setPermisosUsuario(info);
          setSeleccionados(info.permisosAdicionales || []);
        }
      } catch (err) {
        if (!cancelado) setError(mensajeError(err));
      }
    })();
    return () => { cancelado = true; };
  }, [usuarioId]);

  const modulos = useMemo(() => agruparPorModulo(catalogo), [catalogo]);

  const permisosDisponibles = useMemo(() => {
    if (!permisosUsuario) return [];
    const delRol = new Set(permisosUsuario.permisosRol || []);
    return catalogo.filter((item) => !delRol.has(item.codigo));
  }, [catalogo, permisosUsuario]);

  const resumenRoles = useMemo(() => ({
    totalPermisos: catalogo.length,
    admin: roles.find((r) => r.rol === 'ADMIN')?.permisos?.length || 0,
    cajero: roles.find((r) => r.rol === 'CAJERO')?.permisos?.length || 0,
    almacenista: roles.find((r) => r.rol === 'ALMACENISTA')?.permisos?.length || 0,
  }), [catalogo, roles]);

  function togglePermiso(codigo) {
    setSeleccionados((prev) => (
      prev.includes(codigo) ? prev.filter((p) => p !== codigo) : [...prev, codigo]
    ));
  }

  async function guardarAdicionales() {
    if (!usuarioId) return;
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const actualizado = await permisoService.actualizarUsuario(usuarioId, seleccionados);
      setPermisosUsuario(actualizado);
      setSeleccionados(actualizado.permisosAdicionales || []);
      setOk('Permisos adicionales actualizados.');
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  function tienePermisoRol(rol, codigo) {
    const info = roles.find((r) => r.rol === rol);
    return info?.permisos?.includes(codigo);
  }

  return (
    <section className="perm-page">
      <header className="cfg-topbar perm-topbar">
        <div>
          <p className="perm-kicker">Administración</p>
          <h1>Permisos y roles</h1>
          <p>Matriz de permisos base por rol y concesiones adicionales por usuario operativo.</p>
        </div>
      </header>

      <UsuariosSubNav />

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <div className="cat-kpi-grid perm-kpi-grid">
        <article className="cat-kpi">
          <span>Permisos en catálogo</span>
          <strong>{resumenRoles.totalPermisos}</strong>
          <small>Acciones granulares</small>
        </article>
        <article className="cat-kpi">
          <span>Administrador</span>
          <strong>{resumenRoles.admin}</strong>
          <small>Acceso total</small>
        </article>
        <article className="cat-kpi">
          <span>Cajero</span>
          <strong>{resumenRoles.cajero}</strong>
          <small>POS y caja</small>
        </article>
        <article className="cat-kpi">
          <span>Almacenista</span>
          <strong>{resumenRoles.almacenista}</strong>
          <small>Catálogo e inventario</small>
        </article>
      </div>

      <CatalogTabs tabs={TABS.map((t) => [t.id, t.label])} active={tab} onChange={setTab} />

      {cargando ? <p className="placeholder">Cargando permisos…</p> : null}

      {!cargando && tab === 'roles' ? (
        <article className="card perm-matrix-card">
          <div className="perm-table-header">
            <h3>Matriz rol × permiso</h3>
            <span>Los permisos base se definen por rol. El administrador siempre tiene acceso total.</span>
          </div>
          <div className="perm-table-wrap">
            <table className="data-table perm-matrix">
              <thead>
                <tr>
                  <th>Módulo / Permiso</th>
                  <th>Administrador</th>
                  <th>Cajero</th>
                  <th>Almacenista</th>
                </tr>
              </thead>
              <tbody>
                {modulos.map(([modulo, items]) => (
                  items.map((item, idx) => (
                    <tr key={item.codigo}>
                      <td>
                        {idx === 0 ? <span className="perm-modulo">{modulo}</span> : null}
                        <strong>{item.etiqueta}</strong>
                        <small>{item.codigo}</small>
                      </td>
                      <td className="perm-cell-check">
                        {tienePermisoRol('ADMIN', item.codigo) ? <Icon name="check" size={16} className="perm-check-icon" /> : '—'}
                      </td>
                      <td className="perm-cell-check">
                        {tienePermisoRol('CAJERO', item.codigo) ? <Icon name="check" size={16} className="perm-check-icon" /> : '—'}
                      </td>
                      <td className="perm-cell-check">
                        {tienePermisoRol('ALMACENISTA', item.codigo) ? <Icon name="check" size={16} className="perm-check-icon" /> : '—'}
                      </td>
                    </tr>
                  ))
                ))}
              </tbody>
            </table>
          </div>
        </article>
      ) : null}

      {!cargando && tab === 'adicionales' ? (
        <div className="perm-adicional-layout">
          <article className="card perm-user-card">
            <h3>Usuario operativo</h3>
            <label className="perm-user-select">
              Seleccione cajero o almacenista
              <select value={usuarioId} onChange={(e) => setUsuarioId(e.target.value)}>
                <option value="">— Elegir usuario —</option>
                {usuarios.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.nombreCompleto} ({u.username})
                  </option>
                ))}
              </select>
            </label>
            {permisosUsuario ? (
              <div className="perm-user-resumen">
                <UsuarioRolChip rol={permisosUsuario.rol} />
                <p>
                  <strong>{permisosUsuario.nombreCompleto}</strong>
                  {' · '}
                  {ETIQUETA_ROL[permisosUsuario.rol]}
                </p>
                <p className="perm-count">
                  {permisosUsuario.permisosEfectivos?.length || 0} permiso(s) efectivo(s)
                  {' · '}
                  {permisosUsuario.permisosAdicionales?.length || 0} adicional(es)
                </p>
              </div>
            ) : null}
          </article>

          <article className="card perm-checks-card">
            <div className="perm-table-header">
              <h3>Permisos adicionales</h3>
              <span>Solo se pueden otorgar permisos que el rol base no incluye.</span>
            </div>
            {!usuarioId ? (
              <p className="placeholder">Seleccione un usuario para editar permisos extra.</p>
            ) : !permisosDisponibles.length ? (
              <p className="placeholder">No hay permisos adicionales disponibles para este rol.</p>
            ) : (
              <div className="perm-check-grid">
                {permisosDisponibles.map((item) => (
                  <label key={item.codigo} className="perm-check-item">
                    <input
                      type="checkbox"
                      checked={seleccionados.includes(item.codigo)}
                      onChange={() => togglePermiso(item.codigo)}
                    />
                    <span>
                      <strong>{item.etiqueta}</strong>
                      <small>{item.modulo} · {item.codigo}</small>
                    </span>
                  </label>
                ))}
              </div>
            )}
            <div className="perm-actions">
              <Button variant="secondary" onClick={() => setSeleccionados(permisosUsuario?.permisosAdicionales || [])} disabled={!usuarioId || guardando}>
                Deshacer
              </Button>
              <Button onClick={guardarAdicionales} disabled={!usuarioId || guardando}>
                {guardando ? 'Guardando…' : 'Guardar permisos'}
              </Button>
            </div>
          </article>
        </div>
      ) : null}
    </section>
  );
}
