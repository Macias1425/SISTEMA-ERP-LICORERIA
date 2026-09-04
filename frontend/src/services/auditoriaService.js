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

export const auditoriaService = {
  listar: (filtros = {}) => api.get(`/auditoria${construirQuery(filtros)}`),
  resumen: () => api.get('/auditoria/resumen'),
  obtener: (id) => api.get(`/auditoria/${id}`),
};
