/** Códigos alineados con el enum Permiso del backend. */
export const PERMISOS = {
  DASHBOARD_VER: 'DASHBOARD_VER',
  REPORTES_VER: 'REPORTES_VER',
  FINANZAS_VER: 'FINANZAS_VER',
  USUARIOS_VER: 'USUARIOS_VER',
  USUARIOS_GESTIONAR: 'USUARIOS_GESTIONAR',
  PERMISOS_GESTIONAR: 'PERMISOS_GESTIONAR',
  PRODUCTOS_VER: 'PRODUCTOS_VER',
  PRODUCTOS_GESTIONAR: 'PRODUCTOS_GESTIONAR',
  CATEGORIAS_GESTIONAR: 'CATEGORIAS_GESTIONAR',
  PROVEEDORES_GESTIONAR: 'PROVEEDORES_GESTIONAR',
  MARCAS_PRECIOS_GESTIONAR: 'MARCAS_PRECIOS_GESTIONAR',
  INVENTARIO_VER: 'INVENTARIO_VER',
  INVENTARIO_AJUSTAR: 'INVENTARIO_AJUSTAR',
  MERMA_SOLICITAR: 'MERMA_SOLICITAR',
  MERMA_APROBAR: 'MERMA_APROBAR',
  COMPRAS_GESTIONAR: 'COMPRAS_GESTIONAR',
  VENTAS_CREAR: 'VENTAS_CREAR',
  VENTAS_ANULAR: 'VENTAS_ANULAR',
  CAJA_OPERAR: 'CAJA_OPERAR',
  FACTURAS_VER: 'FACTURAS_VER',
  CONTROL_VENTAS_VER: 'CONTROL_VENTAS_VER',
  CONTROL_VENTAS_CONFIG: 'CONTROL_VENTAS_CONFIG',
  CONFIG_GESTIONAR: 'CONFIG_GESTIONAR',
  AUDITORIA_VER: 'AUDITORIA_VER',
  MANTENIMIENTO_GESTIONAR: 'MANTENIMIENTO_GESTIONAR',
};

export const NAV_INICIO = {
  to: '/dashboard',
  label: 'Inicio',
  icono: 'inicio',
  permisos: [PERMISOS.DASHBOARD_VER],
};

/** Menú lateral agrupado por eje funcional. */
export const NAV_GRUPOS = [
  {
    id: 'operaciones',
    label: 'Operaciones',
    enlaces: [
      { to: '/pos', label: 'POS', icono: 'pos', permisos: [PERMISOS.VENTAS_CREAR] },
      { to: '/facturas', label: 'Facturas', icono: 'facturas', permisos: [PERMISOS.FACTURAS_VER] },
      { to: '/control-ventas', label: 'Control ventas', icono: 'control-ventas', permisos: [PERMISOS.CONTROL_VENTAS_VER] },
      { to: '/reportes', label: 'Reportes', icono: 'reportes', permisos: [PERMISOS.REPORTES_VER] },
      { to: '/finanzas', label: 'Finanzas', icono: 'finanzas', permisos: [PERMISOS.FINANZAS_VER] },
    ],
  },
  {
    id: 'inventario',
    label: 'Inventario',
    enlaces: [
      { to: '/inventario', label: 'Inventario', icono: 'inventario', permisos: [PERMISOS.INVENTARIO_VER] },
      { to: '/compras', label: 'Compras', icono: 'proveedores', permisos: [PERMISOS.INVENTARIO_VER, PERMISOS.COMPRAS_GESTIONAR] },
    ],
  },
  {
    id: 'bodega',
    label: 'Bodega',
    enlaces: [
      { to: '/productos', label: 'Productos', icono: 'productos', permisos: [PERMISOS.PRODUCTOS_VER, PERMISOS.PRODUCTOS_GESTIONAR] },
      { to: '/marcas-precios', label: 'Marcas y precios', icono: 'precios', permisos: [PERMISOS.MARCAS_PRECIOS_GESTIONAR] },
      { to: '/categorias', label: 'Categorías', icono: 'categorias', permisos: [PERMISOS.CATEGORIAS_GESTIONAR] },
      { to: '/proveedores', label: 'Proveedores', icono: 'proveedores', permisos: [PERMISOS.PROVEEDORES_GESTIONAR] },
    ],
  },
  {
    id: 'sistema',
    label: 'Sistema',
    enlaces: [
      { to: '/usuarios', label: 'Usuarios', icono: 'usuarios', permisos: [PERMISOS.USUARIOS_VER, PERMISOS.USUARIOS_GESTIONAR] },
      { to: '/configuracion', label: 'Configuración', icono: 'config', permisos: [PERMISOS.CONFIG_GESTIONAR] },
      { to: '/auditoria', label: 'Auditoría', icono: 'auditoria', permisos: [PERMISOS.AUDITORIA_VER] },
      { to: '/mantenimiento', label: 'Mantenimiento', icono: 'mantenimiento', permisos: [PERMISOS.MANTENIMIENTO_GESTIONAR] },
    ],
  },
];

export const NAV_ENLACES = [
  NAV_INICIO,
  ...NAV_GRUPOS.flatMap((grupo) => grupo.enlaces),
];

/** Rutas protegidas: basta con uno de los permisos listados. */
export const RUTAS_PERMISOS = {
  '/dashboard': [PERMISOS.DASHBOARD_VER],
  '/reportes': [PERMISOS.REPORTES_VER],
  '/vencimientos': [PERMISOS.REPORTES_VER],
  '/finanzas': [PERMISOS.FINANZAS_VER],
  '/productos': [PERMISOS.PRODUCTOS_VER, PERMISOS.PRODUCTOS_GESTIONAR],
  '/marcas-precios': [PERMISOS.MARCAS_PRECIOS_GESTIONAR],
  '/proveedores': [PERMISOS.PROVEEDORES_GESTIONAR],
  '/categorias': [PERMISOS.CATEGORIAS_GESTIONAR],
  '/inventario': [PERMISOS.INVENTARIO_VER],
  '/compras': [PERMISOS.INVENTARIO_VER, PERMISOS.COMPRAS_GESTIONAR],
  '/pos': [PERMISOS.VENTAS_CREAR],
  '/facturas': [PERMISOS.FACTURAS_VER],
  '/usuarios': [PERMISOS.USUARIOS_VER, PERMISOS.USUARIOS_GESTIONAR],
  '/usuarios/permisos': [PERMISOS.PERMISOS_GESTIONAR],
  '/control-ventas': [PERMISOS.CONTROL_VENTAS_VER],
  '/configuracion': [PERMISOS.CONFIG_GESTIONAR],
  '/auditoria': [PERMISOS.AUDITORIA_VER],
  '/mantenimiento': [PERMISOS.MANTENIMIENTO_GESTIONAR],
};
