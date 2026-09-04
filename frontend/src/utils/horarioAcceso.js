export function horariosAccesoDefault() {
  return [
    { diaSemana: 'MONDAY', horaInicio: '07:00', horaFin: '19:00', activo: true },
    { diaSemana: 'TUESDAY', horaInicio: '07:00', horaFin: '19:00', activo: true },
    { diaSemana: 'WEDNESDAY', horaInicio: '07:00', horaFin: '19:00', activo: true },
    { diaSemana: 'THURSDAY', horaInicio: '07:00', horaFin: '19:00', activo: true },
    { diaSemana: 'FRIDAY', horaInicio: '07:00', horaFin: '19:00', activo: true },
    { diaSemana: 'SATURDAY', horaInicio: '08:00', horaFin: '14:00', activo: true },
    { diaSemana: 'SUNDAY', horaInicio: '08:00', horaFin: '12:00', activo: false },
  ];
}

export function normalizarHorariosApi(horarios) {
  return (horarios || []).map((horario) => ({
    ...horario,
    horaInicio: String(horario.horaInicio || '08:00').slice(0, 5),
    horaFin: String(horario.horaFin || '18:00').slice(0, 5),
  }));
}

export function resumenHorarioAcceso(usuario) {
  if (!usuario?.horarioAccesoHabilitado) return 'Sin restricción de horario';
  if (usuario.accesoPermitidoAhora) return 'Acceso permitido ahora';
  return 'Fuera del horario de acceso';
}
