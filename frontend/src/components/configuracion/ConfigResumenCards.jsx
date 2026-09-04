import { etiquetaDia } from './HorarioEditor';

function Badge({ activo, warn }) {
  return (
    <span className={`cfg-card-badge${activo ? ' ok' : ''}${warn ? ' warn' : ''}`}>
      {activo ? 'ACTIVO' : 'INACTIVO'}
    </span>
  );
}

function Tarjeta({ titulo, badge, lineas, onEditar }) {
  return (
    <article className="cfg-summary-card card">
      <header className="cfg-card-head">
        <h4>{titulo}</h4>
        {badge}
      </header>
      <ul className="cfg-card-lines">
        {lineas.map(([label, valor]) => (
          <li key={label}>
            <span>{label}</span>
            <strong>{valor}</strong>
          </li>
        ))}
      </ul>
      <button type="button" className="cfg-card-edit" onClick={onEditar}>
        Editar {titulo.toLowerCase()}
      </button>
    </article>
  );
}

export default function ConfigResumenCards({ form, onEditar }) {
  const iva = `${Number(form.tasaIvaPct || 0).toFixed(2)}%`;
  const diasActivos = form.horarios?.filter((h) => h.activo).length ?? 0;
  const primerHorario = form.horarios?.find((h) => h.activo);
  const horarioTexto = form.horarioHabilitado && primerHorario
    ? `${primerHorario.horaInicio} – ${primerHorario.horaFin}`
    : 'Sin restricción';

  return (
    <div className="cfg-summary-grid">
      <Tarjeta
        titulo="Empresa"
        badge={<Badge activo />}
        lineas={[
          ['Nombre', form.nombreNegocio],
          ['Dirección', form.direccionNegocio || 'Sin registrar'],
          ['Teléfono', form.telefonoNegocio || 'Sin registrar'],
        ]}
        onEditar={() => onEditar('empresa')}
      />
      <Tarjeta
        titulo="Normativa"
        badge={<Badge activo={form.horarioHabilitado || form.edadMinimaAlcohol >= 18} />}
        lineas={[
          ['Edad mínima', `${form.edadMinimaAlcohol} años`],
          ['IVA', iva],
          ['Anulación c/ turno', form.requiereTurnoAbiertoParaAnular ? 'Sí' : 'No'],
        ]}
        onEditar={() => onEditar('normativa')}
      />
      <Tarjeta
        titulo="Horarios"
        badge={<Badge activo={form.horarioHabilitado} warn={form.horarioHabilitado && diasActivos === 0} />}
        lineas={[
          ['Control horario', form.horarioHabilitado ? 'Encendido' : 'Apagado'],
          ['Días activos', `${diasActivos} / 7`],
          ['Franja ref.', horarioTexto],
        ]}
        onEditar={() => onEditar('horarios')}
      />
      <Tarjeta
        titulo="Facturación fiscal"
        badge={(
          <Badge
            activo={form.facturacionFiscalHabilitada}
            warn={form.facturacionFiscalHabilitada && form.estadoFiscal?.estado !== 'OK'}
          />
        )}
        lineas={[
          ['Autorización DGI', form.autorizacionDgi ? `…${form.autorizacionDgi.slice(-6)}` : 'Sin registrar'],
          ['Disponibles', form.estadoFiscal ? String(form.estadoFiscal.documentosDisponibles ?? 0) : '—'],
          ['Vigencia', form.fechaLimiteEmision || 'Sin fecha'],
        ]}
        onEditar={() => onEditar('fiscal')}
      />
      <Tarjeta
        titulo="Seguridad"
        badge={<Badge activo />}
        lineas={[
          ['Intentos login', String(form.maxIntentosLogin)],
          ['Bloqueo', `${form.bloqueoMinutos} min`],
          ['Política clave', 'Alta'],
        ]}
        onEditar={() => onEditar('seguridad')}
      />
    </div>
  );
}

export function textoEstadoHorarios(form) {
  if (!form?.horarioHabilitado) return 'Horario de licor desactivado';
  const activos = form.horarios?.filter((h) => h.activo) || [];
  if (!activos.length) return 'Sin días activos';
  const dias = activos.map((h) => etiquetaDia(h.diaSemana)).slice(0, 3).join(', ');
  return `${activos.length} día(s): ${dias}${activos.length > 3 ? '…' : ''}`;
}
