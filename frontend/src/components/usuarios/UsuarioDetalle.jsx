import { etiquetaDia } from '../configuracion/HorarioEditor';
import { etiquetaRol } from './UsuarioRolChip';
import UsuarioEstadoChip from './UsuarioEstadoChip';
import UsuarioRolChip from './UsuarioRolChip';
import Button from '../ui/Button';
import { resumenHorarioAcceso } from '../../utils/horarioAcceso';

function fecha(valor) {
  if (!valor) return 'Nunca';
  return new Date(valor).toLocaleString('es-NI');
}

function hora(valor) {
  if (!valor) return '—';
  return String(valor).slice(0, 5);
}

export default function UsuarioDetalle({
  usuario,
  onEditar,
  onResetPassword,
  onToggleActivo,
  guardando,
}) {
  if (!usuario) {
    return (
      <article className="card fac-detail-panel">
        <p className="placeholder">Seleccione un usuario para ver su perfil, rol y accesos.</p>
      </article>
    );
  }

  const horarioResumen = resumenHorarioAcceso(usuario);
  const diasActivos = (usuario.horarios || []).filter((h) => h.activo);

  return (
    <article className="card fac-detail-panel">
      <header className="fac-detail-head">
        <div>
          <p className="brand-kicker">Perfil de acceso</p>
          <h2>{usuario.nombreCompleto}</h2>
          <p className="fac-detail-meta">@{usuario.username}</p>
        </div>
        <UsuarioRolChip rol={usuario.rol} />
      </header>

      <div className="fac-detail-grid">
        <div><span>Estado</span><UsuarioEstadoChip {...usuario} /></div>
        <div><span>Rol</span><strong>{etiquetaRol(usuario.rol)}</strong></div>
        <div><span>Último acceso</span><strong>{fecha(usuario.ultimoAcceso)}</strong></div>
        <div><span>Clave actualizada</span><strong>{fecha(usuario.passwordActualizadaEn)}</strong></div>
        <div><span>Sesión actual</span><strong>{usuario.esSesionActual ? 'Sí (usted)' : 'No'}</strong></div>
        <div><span>Turno caja</span><strong>{usuario.turnoCajaAbierto ? 'Abierto' : 'Sin turno abierto'}</strong></div>
        <div><span>Horario de acceso</span><strong>{horarioResumen}</strong></div>
      </div>

      {usuario.horarioAccesoHabilitado && diasActivos.length ? (
        <ul className="usr-horario-resumen">
          {diasActivos.map((horario) => (
            <li key={horario.diaSemana}>
              <span>{etiquetaDia(horario.diaSemana)}</span>
              <strong>{hora(horario.horaInicio)} – {hora(horario.horaFin)}</strong>
            </li>
          ))}
        </ul>
      ) : null}

      {usuario.esSesionActual ? (
        <p className="hint">No puede bloquearse ni quitarse el rol de administrador si es el único activo.</p>
      ) : null}
      {!usuario.puedeDesactivar && usuario.activo && !usuario.esSesionActual ? (
        <p className="hint">
          {usuario.turnoCajaAbierto
            ? 'Cierra el turno de caja antes de bloquear esta cuenta.'
            : 'Es el último administrador activo; asigne otro admin antes de bloquearlo.'}
        </p>
      ) : null}

      <footer className="fac-detail-actions">
        <Button type="button" variant="secondary" onClick={() => onEditar?.(usuario)} disabled={guardando}>
          Editar
        </Button>
        <Button
          type="button"
          variant="secondary"
          onClick={() => onResetPassword?.(usuario)}
          disabled={guardando || !usuario.activo}
        >
          Resetear clave
        </Button>
        {usuario.activo ? (
          <Button
            type="button"
            variant="secondary"
            onClick={() => onToggleActivo?.(usuario, false)}
            disabled={guardando || !usuario.puedeDesactivar}
          >
            Bloquear cuenta
          </Button>
        ) : (
          <Button
            type="button"
            onClick={() => onToggleActivo?.(usuario, true)}
            disabled={guardando}
          >
            Reactivar cuenta
          </Button>
        )}
      </footer>
    </article>
  );
}
