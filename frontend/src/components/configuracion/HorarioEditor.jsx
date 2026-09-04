const DIAS = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

export function etiquetaDia(dia) {
  return DIAS[dia] || dia;
}

export function horaInput(valor) {
  if (!valor) return '08:00';
  return String(valor).slice(0, 5);
}

export function formatearActualizado(valor) {
  if (!valor) return 'Sin cambios registrados';
  return new Date(valor).toLocaleString('es-NI');
}

export default function HorarioEditor({ horarios, habilitado, onChange, columnaActivo = 'Venta licor' }) {
  function cambiar(indice, campo, value) {
    onChange?.(horarios.map((horario, i) => (
      i === indice ? { ...horario, [campo]: value } : horario
    )));
  }

  return (
    <div className="cfg-horario-wrap">
      <p className="hint">
        {habilitado
          ? 'Al menos un día debe estar activo y cada franja activa debe cubrir mínimo 1 hora.'
          : 'El horario queda guardado pero no bloquea ventas mientras la normativa esté desactivada.'}
      </p>
      <table className="data-table cfg-horario-table">
        <thead>
          <tr>
            <th>Día</th>
            <th>Desde</th>
            <th>Hasta</th>
            <th>{columnaActivo}</th>
          </tr>
        </thead>
        <tbody>
          {horarios.map((horario, indice) => (
            <tr key={horario.diaSemana} className={horario.activo ? '' : 'cfg-row-off'}>
              <td>
                <strong>{etiquetaDia(horario.diaSemana)}</strong>
              </td>
              <td>
                <input
                  type="time"
                  className="search-input"
                  value={horario.horaInicio}
                  onChange={(event) => cambiar(indice, 'horaInicio', event.target.value)}
                  required
                />
              </td>
              <td>
                <input
                  type="time"
                  className="search-input"
                  value={horario.horaFin}
                  onChange={(event) => cambiar(indice, 'horaFin', event.target.value)}
                  required
                />
              </td>
              <td>
                <label className="cfg-switch">
                  <input
                    type="checkbox"
                    checked={Boolean(horario.activo)}
                    onChange={(event) => cambiar(indice, 'activo', event.target.checked)}
                  />
                  <span>{horario.activo ? 'Permitido' : 'Cerrado'}</span>
                </label>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
