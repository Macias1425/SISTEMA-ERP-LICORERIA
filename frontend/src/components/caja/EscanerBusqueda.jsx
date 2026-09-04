import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import Icon from '../ui/Icon';
import { dinero } from '../../utils/formato';

const MAX_SUGERENCIAS = 8;

/**
 * Interpreta lo que llega del lector: "3*ABC" o "ABC*3" agregan tres unidades.
 * Los lectores envían el código y un Enter, así que el multiplicador lo teclea el cajero.
 */
export function interpretarEntrada(texto) {
  const limpio = texto.trim();
  if (!limpio) return { termino: '', cantidad: 1 };

  const conPrefijo = limpio.match(/^(\d{1,3})\s*[*x]\s*(.+)$/i);
  if (conPrefijo) {
    return { termino: conPrefijo[2].trim(), cantidad: Number(conPrefijo[1]) };
  }
  const conSufijo = limpio.match(/^(.+?)\s*[*x]\s*(\d{1,3})$/i);
  if (conSufijo) {
    return { termino: conSufijo[1].trim(), cantidad: Number(conSufijo[2]) };
  }
  return { termino: limpio, cantidad: 1 };
}

function coincide(producto, termino) {
  const texto = `${producto.nombre} ${producto.codigo} ${producto.marca || ''}`.toLowerCase();
  return texto.includes(termino.toLowerCase());
}

/**
 * Campo único de escaneo y búsqueda del POS. Mantiene el foco donde el cajero lo necesita
 * (F2 o cualquier tecla cuando no está escribiendo en otro campo) y confirma cada lectura.
 * La búsqueda la hace el backend (`onBuscar`) para no cargar el catálogo completo.
 */
export default function EscanerBusqueda({ onBuscar, productos = [], onAgregar, capturaGlobal = true }) {
  const [texto, setTexto] = useState('');
  const [abierto, setAbierto] = useState(false);
  const [ultimo, setUltimo] = useState(null);
  const [noEncontrado, setNoEncontrado] = useState('');
  const [sugerencias, setSugerencias] = useState([]);
  const campo = useRef(null);

  const { termino, cantidad } = useMemo(() => interpretarEntrada(texto), [texto]);

  const buscar = useCallback(async (consulta) => {
    if (typeof onBuscar === 'function') {
      const lista = await onBuscar(consulta);
      return Array.isArray(lista) ? lista : [];
    }
    return productos.filter((producto) => coincide(producto, consulta)).slice(0, MAX_SUGERENCIAS);
  }, [onBuscar, productos]);

  useEffect(() => {
    if (!termino) {
      setSugerencias([]);
      return undefined;
    }
    let cancelado = false;
    const id = window.setTimeout(async () => {
      try {
        const lista = await buscar(termino);
        if (!cancelado) setSugerencias(lista.slice(0, MAX_SUGERENCIAS));
      } catch {
        if (!cancelado) setSugerencias([]);
      }
    }, 200);
    return () => {
      cancelado = true;
      window.clearTimeout(id);
    };
  }, [termino, buscar]);

  const enfocar = useCallback(() => campo.current?.focus(), []);

  useEffect(() => {
    if (!capturaGlobal) return undefined;

    function atajos(evento) {
      if (evento.key === 'F2' && !evento.ctrlKey && !evento.metaKey && !evento.altKey) {
        evento.preventDefault();
        enfocar();
        return;
      }
      const etiqueta = document.activeElement?.tagName;
      const editando = etiqueta === 'INPUT' || etiqueta === 'TEXTAREA' || etiqueta === 'SELECT';
      const imprimible = evento.key.length === 1 && !evento.ctrlKey && !evento.metaKey && !evento.altKey;
      if (!editando && imprimible) {
        enfocar();
      }
    }
    window.addEventListener('keydown', atajos);
    return () => window.removeEventListener('keydown', atajos);
  }, [capturaGlobal, enfocar]);

  function agregar(producto, unidades) {
    onAgregar(producto, unidades);
    setUltimo({ nombre: producto.nombre, cantidad: unidades });
    setNoEncontrado('');
    setTexto('');
    setAbierto(false);
    enfocar();
  }

  async function alPresionarTecla(evento) {
    if (evento.key === 'Escape') {
      setTexto('');
      setAbierto(false);
      return;
    }
    if (evento.key !== 'Enter') return;

    evento.preventDefault();
    if (!termino) return;

    let lista = sugerencias;
    if (!lista.length) {
      try {
        lista = await buscar(termino);
      } catch {
        lista = [];
      }
    }
    const exacto = lista.find((producto) => producto.codigo?.toLowerCase() === termino.toLowerCase());
    const candidato = exacto || (lista.length === 1 ? lista[0] : null);
    if (candidato) {
      agregar(candidato, cantidad);
      return;
    }
    if (lista.length > 1) {
      setSugerencias(lista.slice(0, MAX_SUGERENCIAS));
      setAbierto(true);
      return;
    }
    setNoEncontrado(termino);
    setUltimo(null);
  }

  return (
    <div className="pos-toolbar-search-wrap">
      <label className="pos-toolbar-search">
        <Icon name="search" size={18} strokeWidth={2} />
        <input
          ref={campo}
          type="search"
          placeholder="F2 · Escanear código, buscar producto o 3*código…"
          value={texto}
          onChange={(evento) => {
            setTexto(evento.target.value);
            setAbierto(true);
            setNoEncontrado('');
          }}
          onFocus={() => setAbierto(true)}
          onBlur={() => window.setTimeout(() => setAbierto(false), 150)}
          onKeyDown={alPresionarTecla}
          autoComplete="off"
          autoFocus
        />
        {cantidad > 1 ? <span className="pos-escaner-multiplicador">×{cantidad}</span> : null}
      </label>

      {abierto && sugerencias.length ? (
        <ul className="pos-toolbar-suggest" role="listbox">
          {sugerencias.map((producto) => (
            <li key={producto.id}>
              <button
                type="button"
                role="option"
                onMouseDown={(evento) => evento.preventDefault()}
                onClick={() => agregar(producto, cantidad)}
              >
                <span className="pos-toolbar-suggest-name">{producto.nombre}</span>
                <span className="pos-toolbar-suggest-code">{producto.codigo}</span>
                <strong>{dinero(producto.precioVenta)}</strong>
              </button>
            </li>
          ))}
        </ul>
      ) : null}

      {noEncontrado ? (
        <p className="pos-escaner-feedback error" role="alert">
          Código no reconocido: <code>{noEncontrado}</code>
        </p>
      ) : ultimo ? (
        <p className="pos-escaner-feedback ok" role="status">
          Agregado: {ultimo.nombre}{ultimo.cantidad > 1 ? ` ×${ultimo.cantidad}` : ''}
        </p>
      ) : null}
    </div>
  );
}
