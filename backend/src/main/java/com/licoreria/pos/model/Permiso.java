package com.licoreria.pos.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permiso {

    DASHBOARD_VER("General", "Ver panel de inicio"),
    REPORTES_VER("Reportes", "Ver reportes operativos"),
    FINANZAS_VER("Finanzas", "Ver finanzas y márgenes"),

    USUARIOS_VER("Usuarios", "Ver listado de usuarios"),
    USUARIOS_GESTIONAR("Usuarios", "Crear y editar usuarios"),
    PERMISOS_GESTIONAR("Usuarios", "Gestionar permisos adicionales"),

    PRODUCTOS_VER("Catálogo", "Ver productos"),
    PRODUCTOS_GESTIONAR("Catálogo", "Gestionar productos"),
    CATEGORIAS_GESTIONAR("Catálogo", "Gestionar categorías"),
    PROVEEDORES_GESTIONAR("Catálogo", "Gestionar proveedores"),
    CLIENTES_GESTIONAR("Catálogo", "Gestionar clientes"),
    MARCAS_PRECIOS_GESTIONAR("Catálogo", "Gestionar marcas y precios"),

    INVENTARIO_VER("Inventario", "Ver inventario"),
    INVENTARIO_AJUSTAR("Inventario", "Ajustar stock e inventario"),
    COMPRAS_GESTIONAR("Inventario", "Registrar compras"),
    MERMA_SOLICITAR("Inventario", "Solicitar mermas"),
    MERMA_APROBAR("Inventario", "Aprobar o rechazar mermas"),

    VENTAS_CREAR("Ventas", "Registrar ventas en POS"),
    VENTAS_ANULAR("Ventas", "Anular facturas"),
    VENTAS_OVERRIDE_PRECIO("Ventas", "Autorizar cambio de precio en venta"),
    FACTURAS_VER("Ventas", "Ver facturas emitidas"),
    CAJA_OPERAR("Ventas", "Abrir y cerrar turno de caja"),
    CONTROL_VENTAS_VER("Ventas", "Ver control y supervisión de ventas"),
    CONTROL_VENTAS_CONFIG("Ventas", "Configurar reglas de control de ventas"),

    CONFIG_GESTIONAR("Administración", "Configurar parámetros globales"),
    AUDITORIA_VER("Administración", "Ver auditoría operacional"),
    MANTENIMIENTO_GESTIONAR("Administración", "Respaldos y mantenimiento de BD");

    private final String modulo;
    private final String etiqueta;
}
