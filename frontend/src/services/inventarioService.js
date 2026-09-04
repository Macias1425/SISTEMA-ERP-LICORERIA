import { api } from './api';

function query(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') {
      search.set(clave, valor);
    }
  });
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}

export const inventarioService = {
  alertas: (params = {}) => api.get(`/inventario/alertas${query(params)}`),
  listarMovimientos: (params = {}) => {
    const { productoId, ...resto } = params;
    const qs = query(resto);
    if (productoId) {
      return api.get(`/inventario/movimientos/${productoId}${qs}`);
    }
    return api.get(`/inventario/movimientos${qs}`);
  },
  registrarMovimiento: (movimiento) => api.post('/inventario/movimientos', movimiento),
  lotesDeProducto: (productoId, params = {}) => api.get(`/inventario/lotes/${productoId}${query(params)}`),
  lotesFefo: (productoId, params = {}) => api.get(`/inventario/lotes/${productoId}/fefo${query(params)}`),
  lotesPorVencer: (dias, params = {}) => api.get(`/inventario/lotes/por-vencer${query({ dias, ...params })}`),
  listarMermas: (params = {}) => api.get(`/inventario/mermas${query(params)}`),
  obtenerMerma: (id) => api.get(`/inventario/mermas/${id}`),
  solicitarMerma: (merma) => api.post('/inventario/mermas', merma),
  aprobarMerma: (id, payload) => api.post(`/inventario/mermas/${id}/aprobar`, payload),
  rechazarMerma: (id, payload) => api.post(`/inventario/mermas/${id}/rechazar`, payload),
};
