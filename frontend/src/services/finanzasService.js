import { api } from './api';

function construirQuery(params) {
  const partes = [];
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor != null && String(valor).trim() !== '') {
      partes.push(`${encodeURIComponent(clave)}=${encodeURIComponent(valor)}`);
    }
  });
  return partes.length ? `?${partes.join('&')}` : '';
}

export const finanzasService = {
  periodo: (params = {}) => api.get(`/finanzas/periodo${construirQuery(params)}`),
  margenRiesgo: (params = {}) => api.get(`/finanzas/margen-riesgo${construirQuery(params)}`),
};
