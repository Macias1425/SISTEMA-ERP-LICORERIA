import { api } from './api';

export const alertasService = {
  resumen: () => api.get('/alertas/resumen'),
};
