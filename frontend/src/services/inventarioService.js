import { api } from './api';

export const inventarioService = {
  listarMovimientos: (productoId) => api.get(`/inventario/movimientos/${productoId}`),
  registrarMovimiento: (movimiento) => api.post('/inventario/movimientos', movimiento),
};
