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

export const compraService = {
  listar: (params = {}) => api.get(`/compras${query(params)}`),
  obtener: (id) => api.get(`/compras/${id}`),
  sugerencias: (params = {}) => api.get(`/compras/sugerencias${query(params)}`),
  crearOrden: (compra) => api.post('/compras/orden', compra),
  recibir: (compra) => api.post('/compras', compra),
  recibirOrden: (id, payload) => api.post(`/compras/${id}/recibir`, payload || {}),
  cerrarOrden: (id, payload) => api.post(`/compras/${id}/cerrar`, payload),
  anular: (id, payload) => api.post(`/compras/${id}/anular`, payload),
};
