import { useEffect, useMemo, useState } from 'react';
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import Button from '../ui/Button';
import { stripeService } from '../../services/stripeService';
import { mensajeError } from '../../auth/AuthContext';
import { dinero } from '../../utils/formato';

function StripeCheckoutForm({ total, onPaid, onCancel, disabled }) {
  const stripe = useStripe();
  const elements = useElements();
  const [error, setError] = useState('');
  const [procesando, setProcesando] = useState(false);

  async function confirmar(event) {
    event.preventDefault();
    if (!stripe || !elements || disabled) return;
    setProcesando(true);
    setError('');
    try {
      const resultado = await stripe.confirmPayment({
        elements,
        redirect: 'if_required',
      });
      if (resultado.error) {
        setError(resultado.error.message || 'No se pudo confirmar el pago');
        return;
      }
      const intent = resultado.paymentIntent;
      if (!intent || intent.status !== 'succeeded') {
        setError('El pago no quedó confirmado. Intente de nuevo.');
        return;
      }
      onPaid(intent.id);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setProcesando(false);
    }
  }

  return (
    <form className="pos-stripe-form" onSubmit={confirmar}>
      <PaymentElement options={{ layout: 'tabs' }} />
      {error ? <p className="pos-checkout-alert" role="alert">{error}</p> : null}
      <div className="pos-stripe-actions">
        <Button type="submit" disabled={!stripe || !elements || procesando || disabled}>
          {procesando ? 'Confirmando…' : `Pagar ${dinero(total)}`}
        </Button>
        <Button type="button" variant="secondary" onClick={onCancel} disabled={procesando}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}

/**
 * Cobra con Stripe Payment Element. Solo se monta si Stripe está habilitado en el backend.
 */
export default function StripePaymentBox({
  total,
  publishableKey,
  currency = 'usd',
  disabled = false,
  onPaid,
  onCancel,
}) {
  const [clientSecret, setClientSecret] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);

  const stripePromise = useMemo(
    () => (publishableKey ? loadStripe(publishableKey) : null),
    [publishableKey]
  );

  useEffect(() => {
    let vivo = true;
    setCargando(true);
    setError('');
    setClientSecret('');
    if (!publishableKey || !total || Number(total) <= 0) {
      setCargando(false);
      return undefined;
    }
    stripeService.crearPaymentIntent(Number(total))
      .then((res) => {
        if (vivo) setClientSecret(res.clientSecret);
      })
      .catch((err) => {
        if (vivo) setError(mensajeError(err));
      })
      .finally(() => {
        if (vivo) setCargando(false);
      });
    return () => { vivo = false; };
  }, [publishableKey, total]);

  if (!publishableKey) {
    return <p className="pos-checkout-card-hint">Stripe no está configurado.</p>;
  }
  if (cargando) {
    return <p className="pos-checkout-card-hint">Preparando cobro Stripe…</p>;
  }
  if (error) {
    return <p className="pos-checkout-alert" role="alert">{error}</p>;
  }
  if (!clientSecret || !stripePromise) {
    return <p className="pos-checkout-alert" role="alert">No se pudo iniciar Stripe.</p>;
  }

  return (
    <div className="pos-stripe-box">
      <p className="pos-checkout-card-hint">
        Pago seguro con Stripe ({String(currency).toUpperCase()}). Tarjeta de prueba: 4242…
      </p>
      <Elements
        stripe={stripePromise}
        options={{
          clientSecret,
          appearance: { theme: 'stripe' },
        }}
      >
        <StripeCheckoutForm
          total={total}
          onPaid={onPaid}
          onCancel={onCancel}
          disabled={disabled}
        />
      </Elements>
    </div>
  );
}
