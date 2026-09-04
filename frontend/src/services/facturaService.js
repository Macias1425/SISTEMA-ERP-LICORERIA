import { api } from './api';

function query(params = {}) {
  const q = new URLSearchParams();
  if (params.estado && params.estado !== 'TODAS') q.set('estado', params.estado);
  if (params.busqueda?.trim()) q.set('busqueda', params.busqueda.trim());
  if (params.desde) q.set('desde', params.desde);
  if (params.hasta) q.set('hasta', params.hasta);
  if (params.cajeroId) q.set('cajeroId', params.cajeroId);
  const qs = q.toString();
  return qs ? `?${qs}` : '';
}

export const facturaService = {
  listar: (params) => api.get(`/facturas${query(params)}`),
  obtener: (id) => api.get(`/facturas/${id}`),
  porVenta: (ventaId) => api.get(`/facturas/venta/${ventaId}`),
  emitirDesdeVenta: (ventaId) => api.post(`/facturas/venta/${ventaId}`),
  anular: (id, anulacion) => api.post(`/facturas/${id}/anular`, anulacion),
};
