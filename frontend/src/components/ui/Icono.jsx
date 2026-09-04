export function Icono({ nombre, className }) {
  const props = {
    className,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: '1.7',
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': 'true',
  };

  if (nombre === 'inicio') {
    return (
      <svg {...props}>
        <path d="M4 11 12 4l8 7" />
        <path d="M6 10.5V20h12v-9.5" />
      </svg>
    );
  }
  if (nombre === 'pos') {
    return (
      <svg {...props}>
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="M7 9h10M7 13h6" />
      </svg>
    );
  }
  if (nombre === 'facturas') {
    return (
      <svg {...props}>
        <path d="M7 3h8l4 4v14H7z" />
        <path d="M15 3v4h4M9 12h8M9 16h6" />
      </svg>
    );
  }
  if (nombre === 'inventario') {
    return (
      <svg {...props}>
        <path d="M4 7h16l-1.5 12h-13z" />
        <path d="M9 7V5h6v2" />
      </svg>
    );
  }
  if (nombre === 'productos') {
    return (
      <svg {...props}>
        <path d="M8 4h8l3 6H5z" />
        <path d="M6 10v10h12V10" />
      </svg>
    );
  }
  if (nombre === 'precios') {
    return (
      <svg {...props}>
        <path d="M12 3v3M8 6h8" />
        <path d="M6 10c0 4 2.5 7 6 9 3.5-2 6-5 6-9H6z" />
        <path d="M9 12h6" />
      </svg>
    );
  }
  if (nombre === 'proveedores') {
    return (
      <svg {...props}>
        <path d="M3 9h11v10H3z" />
        <path d="M16 12h5v7h-5z" />
        <path d="M6 13h2M6 16h2M18 15h2" />
      </svg>
    );
  }
  if (nombre === 'categorias') {
    return (
      <svg {...props}>
        <rect x="4" y="4" width="7" height="7" rx="1" />
        <rect x="13" y="4" width="7" height="7" rx="1" />
        <rect x="4" y="13" width="7" height="7" rx="1" />
        <rect x="13" y="13" width="7" height="7" rx="1" />
      </svg>
    );
  }
  if (nombre === 'reportes') {
    return (
      <svg {...props}>
        <path d="M5 19V9M10 19V5M15 19v-7M20 19V8" />
      </svg>
    );
  }
  if (nombre === 'finanzas') {
    return (
      <svg {...props}>
        <path d="M12 3v18M8 7h8M7 12h10M9 17h6" />
      </svg>
    );
  }
  if (nombre === 'usuarios') {
    return (
      <svg {...props}>
        <circle cx="12" cy="8" r="3.2" />
        <path d="M5 19c1.2-3.2 3.6-4.8 7-4.8s5.8 1.6 7 4.8" />
      </svg>
    );
  }
  if (nombre === 'config') {
    return (
      <svg {...props}>
        <circle cx="12" cy="12" r="3" />
        <path d="M12 3.5v2.2M12 18.3v2.2M4.8 6.8l1.6 1.6M17.6 15.6l1.6 1.6M3.5 12h2.2M18.3 12h2.2M4.8 17.2l1.6-1.6M17.6 8.4l1.6-1.6" />
      </svg>
    );
  }
  if (nombre === 'auditoria') {
    return (
      <svg {...props}>
        <path d="M12 4 5 7v5c0 4.2 2.8 7.4 7 8.5 4.2-1.1 7-4.3 7-8.5V7z" />
      </svg>
    );
  }
  if (nombre === 'mantenimiento') {
    return (
      <svg {...props}>
        <ellipse cx="12" cy="6" rx="7" ry="3" />
        <path d="M5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6" />
        <path d="M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6" />
      </svg>
    );
  }
  if (nombre === 'control-ventas') {
    return (
      <svg {...props}>
        <path d="M4 19h16M6 16l3-8 4 5 3-4 2 7" />
        <circle cx="18" cy="6" r="2.5" />
      </svg>
    );
  }
  if (nombre === 'caja') {
    return (
      <svg {...props}>
        <rect x="3" y="8" width="18" height="11" rx="2" />
        <path d="M8 8V6h8v2M12 13h.01" />
      </svg>
    );
  }
  if (nombre === 'alerta') {
    return (
      <svg {...props}>
        <path d="M12 4 3 19h18z" />
        <path d="M12 10v4M12 16h.01" />
      </svg>
    );
  }
  if (nombre === 'campana') {
    return (
      <svg {...props}>
        <path d="M6 16V11a6 6 0 1 1 12 0v5l1.5 2.5H4.5z" />
        <path d="M10 19a2 2 0 0 0 4 0" />
      </svg>
    );
  }
  if (nombre === 'lotes') {
    return (
      <svg {...props}>
        <path d="M4 8.5 12 4l8 4.5v7L12 20l-8-4.5z" />
        <path d="M4 8.5 12 13l8-4.5M12 13v7" />
      </svg>
    );
  }
  if (nombre === 'calendar') {
    return (
      <svg {...props}>
        <rect x="4" y="5" width="16" height="15" rx="2" />
        <path d="M8 3v4M16 3v4M4 10h16" />
      </svg>
    );
  }
  return null;
}
