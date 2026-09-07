import { api } from './api';

export const stripeService = {
  config: () => api.get('/pagos/stripe/config'),
  crearPaymentIntent: (monto) => api.post('/pagos/stripe/payment-intent', { monto }),
};
