import { api } from './api';

export const configuracionService = {
  obtener: () => api.get('/configuracion'),
  /** Datos mínimos del negocio para el ticket: accesible a cualquier usuario autenticado. */
  negocio: () => api.get('/configuracion/negocio'),
  guardar: (configuracion) => api.put('/configuracion', configuracion),
};
