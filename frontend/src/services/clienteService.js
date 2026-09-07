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

export const clienteService = {
  listar: (params = {}) => api.get(`/clientes${query(params)}`),
  obtener: (id) => api.get(`/clientes/${id}`),
  deudores: () => api.get('/clientes/deudores'),
  actualizar: (id, dto) => api.put(`/clientes/${id}`, dto),
  abonar: (id, monto) => api.post(`/clientes/${id}/abonos`, { monto }),
};
