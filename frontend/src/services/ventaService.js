import { api } from './api';

export const ventaService = {
  listar: () => api.get('/ventas'),
  obtener: (id) => api.get(`/ventas/${id}`),
  registrar: (venta) => api.post('/ventas', venta),
};
