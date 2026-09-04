function IconoBuscar() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-3.5-3.5" />
    </svg>
  );
}

function IconoEtiqueta() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M4 12V4h8l8 8-8 8-8-8z" />
      <circle cx="8.5" cy="8.5" r="1.2" fill="currentColor" stroke="none" />
    </svg>
  );
}

function IconoFiltro() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M4 6h16M7 12h10M10 18h4" />
    </svg>
  );
}

export default function CatalogToolbar({
  searchValue,
  onSearchChange,
  onSubmit,
  searchPlaceholder = 'Buscar por nombre o código…',
  children,
  activeCount = 0,
  onClearFilters,
}) {
  function submit(event) {
    event.preventDefault();
    onSubmit?.();
  }

  return (
    <form className="cat-toolbar" onSubmit={submit}>
      <div className="cat-search-wrap">
        <span className="cat-search-icon"><IconoBuscar /></span>
        <input
          type="search"
          value={searchValue}
          onChange={(event) => onSearchChange?.(event.target.value)}
          placeholder={searchPlaceholder}
          aria-label="Buscar"
        />
      </div>

      {children}

      {activeCount > 0 ? (
        <button
          type="button"
          className="cat-filters-active"
          onClick={onClearFilters}
          title="Quitar filtros"
        >
          <IconoFiltro />
          <span>{activeCount} filtro{activeCount === 1 ? '' : 's'}</span>
        </button>
      ) : (
        <span className="cat-filters-idle">
          <IconoFiltro />
          Sin filtros
        </span>
      )}
    </form>
  );
}

export function CatalogFilterSelect({
  icon,
  value,
  onChange,
  children,
  ariaLabel,
}) {
  return (
    <label className="cat-filter-pill">
      <span className="cat-filter-icon">{icon || <IconoEtiqueta />}</span>
      <select
        value={value}
        onChange={(event) => onChange?.(event.target.value)}
        aria-label={ariaLabel}
      >
        {children}
      </select>
    </label>
  );
}

export function CatalogTabs({ tabs, active, onChange }) {
  const normalizados = tabs.map((tab, index) => {
    if (Array.isArray(tab)) {
      return { id: tab[0], label: tab[1] };
    }
    return tab;
  });

  return (
    <div className="cat-tabs" role="tablist">
      {normalizados.map((tab, index) => (
        <button
          key={tab.id ?? String(index)}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          className={`cat-tab${active === tab.id ? ' active' : ''}`}
          onClick={() => onChange?.(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
