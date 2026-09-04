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

export const marcasPreciosService = {
  resumen: () => api.get('/marcas-precios/resumen'),
  marcas: (params = {}) => api.get(`/marcas-precios/marcas${construirQuery(params)}`),
  historial: (params = {}) => api.get(`/marcas-precios/historial${construirQuery(params)}`),
  catalogo: (params = {}) => api.get(`/marcas-precios/catalogo${construirQuery(params)}`),
  renombrarMarca: (marcaActual, marcaNueva) => api.post('/marcas-precios/marcas/renombrar', { marcaActual, marcaNueva }),
};
