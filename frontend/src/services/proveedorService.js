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

export const proveedorService = {
  listar: (params = {}) => api.get(`/proveedores${query(params)}`),
  activos: () => api.get('/proveedores/activos'),
  obtener: (id) => api.get(`/proveedores/${id}`),
  crear: (proveedor) => api.post('/proveedores', proveedor),
  actualizar: (id, proveedor) => api.put(`/proveedores/${id}`, proveedor),
  listarPrecios: (id, soloActivos = false, params = {}) => api.get(`/proveedores/${id}/precios${query({ soloActivos, ...params })}`),
  consultarPrecio: (id, productoId, presentacionId) => (
    api.get(`/proveedores/${id}/precios/consulta?productoId=${productoId}&presentacionId=${presentacionId}`)
  ),
  guardarPrecio: (id, precio) => api.post(`/proveedores/${id}/precios`, precio),
};
