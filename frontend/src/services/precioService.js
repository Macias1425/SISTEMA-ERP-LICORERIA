import { api } from './api';

export const precioService = {
  delProducto: (productoId) => api.get(`/precios?productoId=${productoId}`),
  guardar: (precio) => api.post('/precios', precio),
};
