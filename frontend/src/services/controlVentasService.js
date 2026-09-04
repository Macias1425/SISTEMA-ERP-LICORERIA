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

export const controlVentasService = {
  resumen: (params = {}) => api.get(`/control-ventas/resumen${construirQuery(params)}`),
  eventos: (params = {}) => api.get(`/control-ventas/eventos${construirQuery(params)}`),
  detalleVenta: (id) => api.get(`/control-ventas/ventas/${id}`),
  reglas: () => api.get('/control-ventas/reglas'),
  guardarReglas: (reglas) => api.put('/control-ventas/reglas', reglas),
};
