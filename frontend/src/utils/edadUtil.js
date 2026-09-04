export function hoyLocalIso() {
  const fecha = new Date();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

export function calcularEdad(fechaNacimiento, hoyIso = hoyLocalIso()) {
  if (!fechaNacimiento) {
    return null;
  }
  const nac = new Date(`${fechaNacimiento}T12:00:00`);
  const hoy = new Date(`${hoyIso}T12:00:00`);
  if (Number.isNaN(nac.getTime())) {
    return null;
  }
  if (nac > hoy) {
    return -1;
  }
  let edad = hoy.getFullYear() - nac.getFullYear();
  const mes = hoy.getMonth() - nac.getMonth();
  if (mes < 0 || (mes === 0 && hoy.getDate() < nac.getDate())) {
    edad -= 1;
  }
  return edad;
}

export function verificacionEdadOk(hayAlcohol, confirmaEdad, fechaNacimiento, edadMinima = 18) {
  if (!hayAlcohol) {
    return { ok: true };
  }
  if (confirmaEdad) {
    return { ok: true, modo: 'confirmacion' };
  }
  if (!fechaNacimiento) {
    return {
      ok: false,
      mensaje: `Confirme visualmente ${edadMinima}+ años o ingrese fecha de nacimiento válida`,
    };
  }
  const edad = calcularEdad(fechaNacimiento);
  if (edad === null) {
    return { ok: false, mensaje: 'Fecha de nacimiento inválida' };
  }
  if (edad < 0) {
    return { ok: false, mensaje: 'La fecha de nacimiento no puede ser futura' };
  }
  if (edad < edadMinima) {
    return {
      ok: false,
      mensaje: `Cliente menor de edad (${edad} años). Se requieren ${edadMinima}+`,
    };
  }
  return { ok: true, modo: 'fecha', edad };
}
