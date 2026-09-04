import { clearToken, getToken } from '../auth/session';
import { ApiError } from './api';

const API_URL = import.meta.env.VITE_API_URL || '/api';

async function parseError(response) {
  try {
    const body = await response.json();
    return new ApiError(
      response.status,
      body.codigo || 'ERROR',
      body.mensaje || `Error HTTP ${response.status}`
    );
  } catch {
    return new ApiError(response.status, 'ERROR', `Error HTTP ${response.status}`);
  }
}

function headersJson() {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function headersAuth() {
  const token = getToken();
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function jsonOrThrow(response) {
  if (response.status === 401) clearToken();
  if (!response.ok) throw await parseError(response);
  if (response.status === 204) return null;
  return response.json();
}

export const mantenimientoService = {
  resumen: () => fetch(`${API_URL}/mantenimiento/resumen`, { headers: headersJson() })
    .then(jsonOrThrow),

  listarRespaldos: (params = {}) => {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') search.set(clave, valor);
    });
    const qs = search.toString();
    return fetch(`${API_URL}/mantenimiento/respaldos${qs ? `?${qs}` : ''}`, { headers: headersJson() })
      .then(jsonOrThrow);
  },

  crearRespaldo: () => fetch(`${API_URL}/mantenimiento/respaldos`, {
    method: 'POST',
    headers: headersJson(),
  }).then(jsonOrThrow),

  descargarRespaldo: async (nombre) => {
    const response = await fetch(`${API_URL}/mantenimiento/respaldos/${encodeURIComponent(nombre)}/descargar`, {
      headers: headersAuth(),
    });
    if (response.status === 401) clearToken();
    if (!response.ok) throw await parseError(response);
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    URL.revokeObjectURL(url);
  },

  eliminarRespaldo: (nombre) => fetch(`${API_URL}/mantenimiento/respaldos/${encodeURIComponent(nombre)}`, {
    method: 'DELETE',
    headers: headersAuth(),
  }).then(jsonOrThrow),

  restaurar: (archivo, confirmacion) => {
    const formData = new FormData();
    formData.append('archivo', archivo);
    formData.append('confirmacion', confirmacion);
    return fetch(`${API_URL}/mantenimiento/restaurar`, {
      method: 'POST',
      headers: headersAuth(),
      body: formData,
    }).then(jsonOrThrow);
  },

  restaurarExistente: (nombre, confirmacion) => {
    const search = new URLSearchParams({ confirmacion });
    return fetch(`${API_URL}/mantenimiento/respaldos/${encodeURIComponent(nombre)}/restaurar?${search}`, {
      method: 'POST',
      headers: headersJson(),
    }).then(jsonOrThrow);
  },

  optimizar: (soloFragmentadas = false) => {
    const search = new URLSearchParams({ soloFragmentadas: String(Boolean(soloFragmentadas)) });
    return fetch(`${API_URL}/mantenimiento/optimizar?${search}`, {
      method: 'POST',
      headers: headersJson(),
    }).then(jsonOrThrow);
  },

  verificar: () => fetch(`${API_URL}/mantenimiento/verificar`, {
    method: 'POST',
    headers: headersJson(),
  }).then(jsonOrThrow),
};
