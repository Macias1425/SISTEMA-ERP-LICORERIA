import Button from './Button';

export default function Pagination({
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  tamano = 20,
  onChange,
  cargando = false,
}) {
  if (totalElementos <= 0) {
    return null;
  }

  const inicio = totalElementos === 0 ? 0 : pagina * tamano + 1;
  const fin = Math.min((pagina + 1) * tamano, totalElementos);

  return (
    <div className="paginacion">
      <span className="paginacion-info">
        {inicio}–{fin} de {totalElementos}
      </span>
      <div className="paginacion-controles">
        <Button
          variant="secondary"
          type="button"
          disabled={cargando || pagina <= 0}
          onClick={() => onChange?.(pagina - 1)}
        >
          Anterior
        </Button>
        <span className="paginacion-pagina">
          Página {pagina + 1} de {Math.max(totalPaginas, 1)}
        </span>
        <Button
          variant="secondary"
          type="button"
          disabled={cargando || pagina + 1 >= totalPaginas}
          onClick={() => onChange?.(pagina + 1)}
        >
          Siguiente
        </Button>
      </div>
    </div>
  );
}
