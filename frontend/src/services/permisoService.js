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

export const permisoService = {
  catalogo: () => api.get('/permisos/catalogo'),
  roles: () => api.get('/permisos/roles'),
  rol: (rol) => api.get(`/permisos/roles/${rol}`),
  usuario: (id) => api.get(`/permisos/usuarios/${id}`),
  actualizarUsuario: (id, permisosAdicionales) => api.put(`/permisos/usuarios/${id}`, { permisosAdicionales }),
};
