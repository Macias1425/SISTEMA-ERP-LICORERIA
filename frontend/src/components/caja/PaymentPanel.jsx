import Button from '../ui/Button';

export default function PaymentPanel() {
  return (
    <section className="payment-panel" style={{ marginTop: '1rem' }}>
      <h3>Cobro</h3>
      <p className="placeholder">Efectivo, tarjeta o crédito mayorista. Pendiente de `ventaService`.</p>
      <Button>Cobrar</Button>
    </section>
  );
}
