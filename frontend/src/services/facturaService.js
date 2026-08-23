import { api } from './api';

export const facturaService = {
  listar: () => api.get('/facturas'),
  emitirDesdeVenta: (ventaId) => api.post(`/facturas/venta/${ventaId}`),
};
