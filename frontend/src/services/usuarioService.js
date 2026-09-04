import { api } from './api';

function construirQuery(params) {
  const partes = [];
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor == null || valor === '') return;
    if (Array.isArray(valor)) {
      valor.forEach((item) => {
        if (item != null && String(item).trim() !== '') {
          partes.push(`${encodeURIComponent(clave)}=${encodeURIComponent(item)}`);
        }
      });
      return;
    }
    if (String(valor).trim() !== '') {
      partes.push(`${encodeURIComponent(clave)}=${encodeURIComponent(valor)}`);
    }
  });
  return partes.length ? `?${partes.join('&')}` : '';
}

export const usuarioService = {
  listar: (params = {}) => api.get(`/usuarios${construirQuery(params)}`),
  obtener: (id) => api.get(`/usuarios/${id}`),
  crear: (usuario) => api.post('/usuarios', usuario),
  actualizar: (id, usuario) => api.put(`/usuarios/${id}`, usuario),
  resetearPassword: (id, passwordNueva) => api.post(`/usuarios/${id}/reset-password`, { passwordNueva }),
};
