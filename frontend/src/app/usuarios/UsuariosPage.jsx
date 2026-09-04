import { useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import CatalogToolbar, { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import UsuariosSubNav from '../../components/usuarios/UsuariosSubNav';
import ResetPasswordModal from '../../components/usuarios/ResetPasswordModal';
import UsuarioDetalleModal from '../../components/usuarios/UsuarioDetalleModal';
import UsuarioEstadoChip from '../../components/usuarios/UsuarioEstadoChip';
import UsuarioFormModal from '../../components/usuarios/UsuarioFormModal';
import UsuarioRolChip from '../../components/usuarios/UsuarioRolChip';
import { usuarioService } from '../../services/usuarioService';
import Pagination from '../../components/ui/Pagination';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

function parseActivoFiltro(valor) {
  if (valor === 'ACTIVOS' || valor === true) return true;
  if (valor === 'INACTIVOS' || valor === false) return false;
  return undefined;
}

function IconoRol() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4z" />
      <path d="M4 20a8 8 0 0 1 16 0" />
    </svg>
  );
}

function IconoEstado() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <circle cx="12" cy="12" r="8" />
      <path d="M8 12h8" />
    </svg>
  );
}

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState([]);
  const [seleccionado, setSeleccionado] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [filtroRol, setFiltroRol] = useState('');
  const [filtroActivo, setFiltroActivo] = useState('ACTIVOS');
  const [tab, setTab] = useState('todos');
  const [modalForm, setModalForm] = useState(false);
  const [modalDetalle, setModalDetalle] = useState(false);
  const [modalReset, setModalReset] = useState(false);
  const [editando, setEditando] = useState(null);
  const [resetObjetivo, setResetObjetivo] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  function paramsListado(params = {}) {
    const tabActual = params.tab ?? tab;
    const activo = params.activo !== undefined ? parseActivoFiltro(params.activo) : parseActivoFiltro(filtroActivo);
    let rol = (params.rol ?? filtroRol) || undefined;
    let roles;
    let debeCambiarPassword;
    let activoFinal = activo;
    if (tabActual === 'admins') rol = 'ADMIN';
    if (tabActual === 'operativos') {
      rol = undefined;
      roles = ['CAJERO', 'ALMACENISTA'];
    }
    if (tabActual === 'claves') debeCambiarPassword = true;
    if (tabActual === 'bloqueados') activoFinal = false;
    return {
      busqueda: params.busqueda ?? busqueda,
      rol,
      roles,
      activo: activoFinal,
      debeCambiarPassword,
    };
  }

  async function cargar(params = {}, paginaDestino = 0) {
    setCargando(true);
    setError('');
    try {
      const respuesta = await usuarioService.listar({
        ...paramsListado(params),
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setUsuarios(contenidoPagina(respuesta));
      setPaginaMeta(metaPagina(respuesta));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  const usuariosVista = usuarios;

  const resumen = useMemo(() => ({
    total: paginaMeta.totalElementos,
    activos: usuarios.filter((u) => u.activo).length,
    admins: usuarios.filter((u) => u.rol === 'ADMIN' && u.activo).length,
    clavesPendientes: usuarios.filter((u) => u.debeCambiarPassword).length,
  }), [usuarios, paginaMeta.totalElementos]);

  const filtrosActivos = useMemo(() => {
    let n = 0;
    if (busqueda.trim()) n += 1;
    if (filtroRol) n += 1;
    if (filtroActivo && filtroActivo !== 'ACTIVOS') n += 1;
    if (tab !== 'todos') n += 1;
    return n;
  }, [busqueda, filtroRol, filtroActivo, tab]);

  function limpiarFiltros() {
    setBusqueda('');
    setFiltroRol('');
    setFiltroActivo('ACTIVOS');
    setTab('todos');
    cargar({ busqueda: '', rol: '', activo: 'ACTIVOS', tab: 'todos' });
  }

  async function ver(usuario) {
    setError('');
    try {
      setSeleccionado(await usuarioService.obtener(usuario.id));
    } catch {
      setSeleccionado(usuario);
    }
    setModalDetalle(true);
  }

  async function guardarUsuario(payload) {
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const guardado = editando
        ? await usuarioService.actualizar(editando.id, payload)
        : await usuarioService.crear(payload);
      setModalForm(false);
      setEditando(null);
      setSeleccionado(guardado);
      setOk(editando ? 'Usuario actualizado.' : 'Usuario creado. Deberá cambiar la clave en su primer ingreso.');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function resetearClave(passwordNueva) {
    if (!resetObjetivo) return;
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const actualizado = await usuarioService.resetearPassword(resetObjetivo.id, passwordNueva);
      setModalReset(false);
      setResetObjetivo(null);
      setSeleccionado(actualizado);
      setOk('Contraseña temporal asignada.');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function toggleActivo(usuario, activo) {
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const actualizado = await usuarioService.actualizar(usuario.id, {
        nombreCompleto: usuario.nombreCompleto,
        rol: usuario.rol,
        activo,
      });
      setSeleccionado(actualizado);
      setOk(activo ? 'Cuenta reactivada.' : 'Cuenta bloqueada.');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="usuarios-page">
      <header className="page-header fac-header">
        <div>
          <h1>Gestión de usuarios</h1>
          <p>Altas, roles, bloqueo de cuentas y reseteo de contraseñas. Solo administradores gestionan el personal.</p>
        </div>
        <button type="button" className="btn primary" onClick={() => { setEditando(null); setModalForm(true); }}>
          + Nuevo usuario
        </button>
      </header>

      <UsuariosSubNav />

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <div className="cat-kpi-grid">
        <article className="cat-kpi">
          <span>Personal</span>
          <strong>{resumen.total}</strong>
          <small>{resumen.activos} activo(s)</small>
        </article>
        <article className="cat-kpi">
          <span>Administradores</span>
          <strong>{resumen.admins}</strong>
          <small>Con acceso total</small>
        </article>
        <article className="cat-kpi">
          <span>Claves pendientes</span>
          <strong>{resumen.clavesPendientes}</strong>
          <small>Deben cambiar al entrar</small>
        </article>
        <article className="cat-kpi">
          <span>En pantalla</span>
          <strong>{usuariosVista.length}</strong>
          <small>Con filtros actuales</small>
        </article>
      </div>

      <CatalogTabs
        tabs={[
          ['todos', 'Todos'],
          ['operativos', 'Cajeros y almacén'],
          ['admins', 'Administradores'],
          ['claves', 'Clave temporal'],
          ['bloqueados', 'Bloqueados'],
        ]}
        active={tab}
        onChange={(valor) => { setTab(valor); cargar({ tab: valor }); }}
      />

      <CatalogToolbar
        searchValue={busqueda}
        onSearchChange={setBusqueda}
        onSubmit={() => cargar()}
        searchPlaceholder="Buscar por usuario o nombre…"
        activeCount={filtrosActivos}
        onClearFilters={limpiarFiltros}
      >
        <CatalogFilterSelect icon={<IconoRol />} value={filtroRol} onChange={(valor) => { setFiltroRol(valor); cargar({ rol: valor }); }} ariaLabel="Rol">
          <option value="">Rol: todos</option>
          <option value="ADMIN">Administrador</option>
          <option value="CAJERO">Cajero</option>
          <option value="ALMACENISTA">Almacenista</option>
        </CatalogFilterSelect>
        <CatalogFilterSelect icon={<IconoEstado />} value={filtroActivo} onChange={(valor) => { setFiltroActivo(valor); cargar({ activo: valor }); }} ariaLabel="Estado">
          <option value="ACTIVOS">Activos</option>
          <option value="TODOS">Todos</option>
          <option value="INACTIVOS">Bloqueados</option>
        </CatalogFilterSelect>
      </CatalogToolbar>

      <article className="card">
        <h3>Personal registrado</h3>
        {cargando ? <p className="placeholder">Cargando…</p> : !usuariosVista.length ? (
          <p className="placeholder">No hay usuarios con los filtros actuales.</p>
        ) : (
          <div className="fac-table-wrap">
            <table className="data-table fac-table cat-table">
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Nombre</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th className="no-print">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {usuariosVista.map((usuario) => (
                  <tr key={usuario.id}>
                    <td>
                      <strong>{usuario.username}</strong>
                      {usuario.esSesionActual ? <span className="cat-badge cat-badge-info">Usted</span> : null}
                    </td>
                    <td>{usuario.nombreCompleto}</td>
                    <td><UsuarioRolChip rol={usuario.rol} /></td>
                    <td><UsuarioEstadoChip {...usuario} /></td>
                    <td className="no-print">
                      <button type="button" className="btn link" onClick={() => ver(usuario)}>
                        Ver detalle
                      </button>
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
          onChange={(nueva) => cargar({}, nueva)}
        />
      </article>

      <UsuarioDetalleModal
        open={modalDetalle}
        usuario={seleccionado}
        guardando={guardando}
        onClose={() => setModalDetalle(false)}
        onEditar={(usuario) => { setEditando(usuario); setModalForm(true); }}
        onResetPassword={(usuario) => { setResetObjetivo(usuario); setModalReset(true); }}
        onToggleActivo={toggleActivo}
      />

      <UsuarioFormModal
        open={modalForm}
        usuario={editando}
        guardando={guardando}
        onClose={() => { setModalForm(false); setEditando(null); }}
        onConfirmar={guardarUsuario}
      />

      <ResetPasswordModal
        open={modalReset}
        usuario={resetObjetivo}
        guardando={guardando}
        onClose={() => { setModalReset(false); setResetObjetivo(null); }}
        onConfirmar={resetearClave}
      />
    </section>
  );
}
