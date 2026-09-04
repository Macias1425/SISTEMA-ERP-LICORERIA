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

export const categoriaService = {
  listar: (params = {}) => api.get(`/categorias${query(params)}`),
  crear: (categoria) => api.post('/categorias', categoria),
  actualizar: (id, categoria) => api.put(`/categorias/${id}`, categoria),
};
