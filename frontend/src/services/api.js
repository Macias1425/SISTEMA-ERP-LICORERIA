import { clearToken, getToken } from '../auth/session';

const API_URL = import.meta.env.VITE_API_URL || '/api';

export class ApiError extends Error {
  constructor(status, codigo, mensaje) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigo;
  }
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

async function request(path, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers,
    });
  } catch {
    throw new ApiError(0, 'SIN_CONEXION', 'No se pudo conectar con el servidor');
  }

  if (response.status === 401 && path !== '/auth/login') {
    clearToken();
  }

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  del: (path) => request(path, { method: 'DELETE' }),
};
