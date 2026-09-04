import { useEffect, useMemo, useRef, useState } from 'react';

import { mensajeError } from '../auth/AuthContext';
import { ventaService } from '../services/ventaService';

const RETARDO_MS = 300;

function clavePeticion(lineas, tipoCliente) {
  const detalle = lineas
    .map((linea) => `${linea.producto.id}:${linea.presentacion.id}:${linea.cantidad}`)
    .join('|');
  return `${tipoCliente}#${detalle}`;
}

/**
 * Cotiza el carrito en el servidor, que es quien manda en precios (listas, mayorista, IVA).
 * El POS solo pinta: así el total en pantalla es el mismo que se cobrará.
 */
export function useCotizacion({ lineas, tipoCliente }) {
  const [cotizacion, setCotizacion] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const ultimaPeticion = useRef(0);

  const clave = clavePeticion(lineas, tipoCliente);

  useEffect(() => {
    if (!lineas.length) {
      setCotizacion(null);
      setError('');
      return undefined;
    }

    const peticion = ultimaPeticion.current + 1;
    ultimaPeticion.current = peticion;
    setCargando(true);

    const temporizador = setTimeout(async () => {
      try {
        const respuesta = await ventaService.cotizar({
          tipoCliente,
          detalles: lineas.map((linea) => ({
            productoId: linea.producto.id,
            presentacionId: linea.presentacion.id,
            cantidad: linea.cantidad,
          })),
        });
        // Descarta respuestas de carritos que el cajero ya modificó.
        if (peticion === ultimaPeticion.current) {
          setCotizacion(respuesta);
          setError('');
        }
      } catch (err) {
        if (peticion === ultimaPeticion.current) {
          setCotizacion(null);
          setError(mensajeError(err));
        }
      } finally {
        if (peticion === ultimaPeticion.current) {
          setCargando(false);
        }
      }
    }, RETARDO_MS);

    return () => clearTimeout(temporizador);
    // `clave` resume el carrito: evita recotizar cuando solo cambia la identidad del array.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave]);

  /** Precio y avisos por línea, indexados por producto+presentación. */
  const lineasPorClave = useMemo(() => {
    const indice = new Map();
    (cotizacion?.lineas || []).forEach((linea) => {
      indice.set(`${linea.productoId}-${linea.presentacionId}`, linea);
    });
    return indice;
  }, [cotizacion]);

  return { cotizacion, lineasCotizadas: lineasPorClave, cargando, error };
}
