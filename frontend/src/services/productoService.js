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

export const productoService = {
  listar: (params = {}) => api.get(`/productos${query(params)}`),
  pos: (params = {}) => api.get(`/productos/pos${query(params)}`),
  obtener: (id) => api.get(`/productos/${id}`),
  crear: (producto) => api.post('/productos', producto),
  actualizar: (id, producto) => api.put(`/productos/${id}`, producto),
  eliminar: (id) => api.del(`/productos/${id}`),
  agregarPresentacion: (id, presentacion) => api.post(`/productos/${id}/presentaciones`, presentacion),
  actualizarPresentacion: (id, presentacionId, presentacion) =>
    api.put(`/productos/${id}/presentaciones/${presentacionId}`, presentacion),
};
