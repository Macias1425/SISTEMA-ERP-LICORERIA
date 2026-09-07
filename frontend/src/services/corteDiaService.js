import { api, ApiError } from './api';
import { clearToken, getToken } from '../auth/session';

const API_URL = import.meta.env.VITE_API_URL || '/api';

function construirQuery(params) {
  const partes = [];
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor != null && String(valor).trim() !== '') {
      partes.push(`${encodeURIComponent(clave)}=${encodeURIComponent(valor)}`);
    }
  });
  return partes.length ? `?${partes.join('&')}` : '';
}

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

export async function cargarCorteDia(fecha) {
  return api.get(`/reportes/corte${construirQuery({ fecha })}`);
}

export async function descargarCortePdf(fecha) {
  const token = getToken();
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${API_URL}/reportes/corte.pdf${construirQuery({ fecha })}`, { headers });
  if (response.status === 401) clearToken();
  if (!response.ok) throw await parseError(response);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `corte-dia-${fecha || 'hoy'}.pdf`;
  enlace.click();
  URL.revokeObjectURL(url);
}
