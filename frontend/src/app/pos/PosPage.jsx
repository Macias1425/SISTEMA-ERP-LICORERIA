import ProductGrid from '../../components/caja/ProductGrid';
import Cart from '../../components/caja/Cart';
import PaymentPanel from '../../components/caja/PaymentPanel';

export default function PosPage() {
  return (
    <section>
      <header className="page-header">
        <h1>Punto de venta</h1>
        <p>Caja para venta minorista y mayorista de productos sellados.</p>
      </header>
      <div className="pos-layout">
        <ProductGrid />
        <div>
          <Cart />
          <PaymentPanel />
        </div>
      </div>
    </section>
  );
}
