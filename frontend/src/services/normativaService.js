import { api } from './api';

export const normativaService = {
  estado: () => api.get('/normativa/estado'),
};
