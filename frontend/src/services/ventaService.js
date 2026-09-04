import { api } from './api';

function query(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') {
      search.set(clave, valor);
    }
  });
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}

export const ventaService = {
  listar: (params = {}) => api.get(`/ventas${query(params)}`),
  delTurno: (turnoId, params = {}) => api.get(`/ventas/turno/${turnoId}${query(params)}`),
  obtener: (id) => api.get(`/ventas/${id}`),
  registrar: (venta) => api.post('/ventas', venta),
  /** Precios y avisos calculados por el servidor antes de cobrar. */
  cotizar: (cotizacion) => api.post('/ventas/cotizar', cotizacion),
};
