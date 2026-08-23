import { api } from './api';

export const productoService = {
  listar: () => api.get('/productos'),
  obtener: (id) => api.get(`/productos/${id}`),
  crear: (producto) => api.post('/productos', producto),
  actualizar: (id, producto) => api.put(`/productos/${id}`, producto),
  eliminar: (id) => api.del(`/productos/${id}`),
};
