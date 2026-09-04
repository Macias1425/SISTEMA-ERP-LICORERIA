import { useCallback, useEffect, useRef, useState } from 'react';

import { alertasService } from '../services/alertasService';

const INTERVALO_MS = 60_000;

const VACIO = { total: 0, criticas: 0, secciones: [] };

/**
 * Alertas operativas del servidor para el badge del menú y el centro de avisos.
 * Un fallo de red deja el último resumen conocido: el aviso nunca debe romper el shell.
 */
export function useAlertasActivas({ activo = true, intervaloMs = INTERVALO_MS } = {}) {
  const [resumen, setResumen] = useState(VACIO);
  const [cargando, setCargando] = useState(false);
  const montado = useRef(true);

  const refrescar = useCallback(async () => {
    setCargando(true);
    try {
      const datos = await alertasService.resumen();
      if (montado.current) {
        setResumen({ ...VACIO, ...datos });
      }
    } catch {
      // Silencio intencional: el resumen previo sigue siendo válido para el usuario.
    } finally {
      if (montado.current) {
        setCargando(false);
      }
    }
  }, []);

  useEffect(() => {
    montado.current = true;
    return () => {
      montado.current = false;
    };
  }, []);

  useEffect(() => {
    if (!activo) return undefined;
    refrescar();
    const temporizador = setInterval(refrescar, intervaloMs);
    return () => clearInterval(temporizador);
  }, [activo, intervaloMs, refrescar]);

  return { resumen, cargando, refrescar };
}

/** Índice ruta → alerta, para pintar el contador junto a cada enlace del menú. */
export function alertasPorRuta(resumen) {
  const indice = new Map();
  (resumen?.secciones || []).forEach((seccion) => {
    const previa = indice.get(seccion.ruta);
    if (!previa) {
      indice.set(seccion.ruta, { cantidad: seccion.cantidad, nivel: seccion.nivel });
      return;
    }
    indice.set(seccion.ruta, {
      cantidad: previa.cantidad + seccion.cantidad,
      nivel: previa.nivel === 'CRITICA' || seccion.nivel === 'CRITICA' ? 'CRITICA' : previa.nivel,
    });
  });
  return indice;
}
