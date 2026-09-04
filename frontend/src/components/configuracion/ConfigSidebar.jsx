const SECCIONES = [
  { id: 'resumen', label: 'Resumen', icon: 'grid' },
  { id: 'empresa', label: 'Empresa', icon: 'building' },
  { id: 'normativa', label: 'Normativa', icon: 'scale' },
  { id: 'horarios', label: 'Horarios', icon: 'clock' },
  { id: 'fiscal', label: 'Facturación fiscal', icon: 'receipt' },
  { id: 'seguridad', label: 'Seguridad', icon: 'shield' },
];
function Icono({ name }) {
  if (name === 'building') {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <path d="M4 20V6l8-4 8 4v14H4z" />
        <path d="M9 10h2M13 10h2M9 14h2M13 14h2M9 18h2M13 18h2" />
      </svg>
    );
  }
  if (name === 'scale') {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <path d="M12 3v18M5 7h14M7 7l-2 6h4l-2-6M17 7l-2 6h4l-2-6" />
      </svg>
    );
  }
  if (name === 'clock') {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <circle cx="12" cy="12" r="8" />
        <path d="M12 8v4l3 2" />
      </svg>
    );
  }
  if (name === 'receipt') {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3z" />
        <path d="M9 8h6M9 12h6M9 16h3" />
      </svg>
    );
  }
  if (name === 'shield') {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <path d="M12 3l8 3v6c0 5-3.5 8.5-8 9-4.5-.5-8-4-8-9V6l8-3z" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <rect x="4" y="4" width="7" height="7" rx="1" />
      <rect x="13" y="4" width="7" height="7" rx="1" />
      <rect x="4" y="13" width="7" height="7" rx="1" />
      <rect x="13" y="13" width="7" height="7" rx="1" />
    </svg>
  );
}

export default function ConfigSidebar({ seccion, busqueda, onBusqueda, onSeccion }) {
  const filtradas = SECCIONES.filter((item) =>
    item.label.toLowerCase().includes((busqueda || '').trim().toLowerCase()));

  return (
    <aside className="cfg-sidebar card">
      <label className="cfg-sidebar-search">
        <span className="cfg-search-icon" aria-hidden="true">⌕</span>
        <input
          type="search"
          value={busqueda}
          onChange={(event) => onBusqueda?.(event.target.value)}
          placeholder="Buscar sección…"
        />
      </label>
      <nav className="cfg-nav" aria-label="Secciones de configuración">
        {filtradas.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`cfg-nav-item${seccion === item.id ? ' active' : ''}`}
            onClick={() => onSeccion?.(item.id)}
          >
            <span className="cfg-nav-icon"><Icono name={item.icon} /></span>
            <span>{item.label}</span>
          </button>
        ))}
      </nav>
    </aside>
  );
}

export { SECCIONES };
