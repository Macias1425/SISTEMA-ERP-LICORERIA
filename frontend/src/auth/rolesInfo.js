import { PERMISOS, NAV_ENLACES } from './permisos';

/** Permisos base del rol cajero (solo POS + caja). */
export const PERMISOS_CAJERO = [
  PERMISOS.VENTAS_CREAR,
  PERMISOS.CAJA_OPERAR,
];

export const CAPACIDADES_ROL = {
  ADMIN: {
    titulo: 'Administrador',
    resumen: 'Acceso total al sistema',
    items: [
      'Usuarios, permisos y configuración',
      'Auditoría y mantenimiento',
      'Todos los módulos operativos',
    ],
  },
  CAJERO: {
    titulo: 'Cajero',
    resumen: 'Punto de venta y turno de caja',
    items: [
      'Punto de venta (POS)',
      'Abrir y cerrar turno de caja',
      'Otros módulos solo con permisos adicionales',
    ],
  },
  ALMACENISTA: {
    titulo: 'Almacenista',
    resumen: 'Catálogo, stock y compras',
    items: [
      'Productos, categorías y proveedores',
      'Inventario, kardex y mermas',
      'Registrar y recibir compras',
      'Reportes y finanzas',
    ],
  },
};

/** Primera ruta accesible según permisos efectivos del usuario. */
export function rutaInicioPorPermisos(usuario) {
  if (!usuario) return '/login';
  const efectivos = new Set(usuario.permisosEfectivos || []);
  if (usuario.rol === 'ADMIN') return '/dashboard';
  for (const enlace of NAV_ENLACES) {
    if (enlace.permisos.some((permiso) => efectivos.has(permiso))) {
      return enlace.to;
    }
  }
  return '/pos';
}

/** Ruta principal tras login según rol. */
export function rutaInicioPorRol(rol) {
  switch (rol) {
    case 'CAJERO':
      return '/pos';
    case 'ALMACENISTA':
      return '/inventario';
    default:
      return '/dashboard';
  }
}
