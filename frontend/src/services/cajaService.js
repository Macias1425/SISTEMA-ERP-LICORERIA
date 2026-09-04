import { api } from './api';

export const cajaService = {
  estado: () => api.get('/caja/estado'),
  abierta: () => api.get('/caja/abierta'),
  abrir: (apertura) => api.post('/caja/abrir', apertura),
  cerrar: (turnoId, cierre) => api.post(`/caja/${turnoId}/cerrar`, cierre),
  historial: (params = {}) => {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') search.set(clave, valor);
    });
    const qs = search.toString();
    return api.get(`/caja/historial${qs ? `?${qs}` : ''}`);
  },
};
