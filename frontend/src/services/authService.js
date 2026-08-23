import { api } from './api';

export const authService = {
  login: (credentials) => api.post('/auth/login', credentials),
  me: () => api.get('/auth/me'),
  cambiarPassword: (payload) => api.post('/auth/cambiar-password', payload),
};
