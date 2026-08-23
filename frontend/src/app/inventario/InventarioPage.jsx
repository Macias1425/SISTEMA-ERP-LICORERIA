export default function InventarioPage() {
  return (
    <section>
      <header className="page-header">
        <h1>Inventario</h1>
        <p>Catálogo de productos sellados, existencias y movimientos.</p>
      </header>
      <article className="card">
        <p className="placeholder">
          Aquí se listarán productos, stock actual y kardex. Conecta `productoService` e `inventarioService`.
        </p>
      </article>
    </section>
  );
}
