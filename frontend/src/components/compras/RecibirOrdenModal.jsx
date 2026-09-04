import { useEffect, useMemo, useState } from 'react';
import Button from '../ui/Button';
import Icon from '../ui/Icon';
import Modal from '../ui/Modal';
import ImpactoRecepcionPanel from './ImpactoRecepcionPanel';
import { dinero } from '../../utils/formato';

function lineaVacia(detalle) {
  const pendiente = detalle.cantidadPendiente ?? Math.max(
    0,
    (detalle.cantidadOrdenada ?? detalle.cantidad ?? 0)
      - (detalle.cantidadRecibida ?? 0)
      - (detalle.cantidadRechazada ?? 0),
  );
  return {
    detalleId: detalle.id,
    productoId: detalle.productoId,
    presentacionId: detalle.presentacionId,
    cantidadRecibida: pendiente,
    cantidadRechazada: 0,
    notasQc: '',
    pendiente,
  };
}

export default function RecibirOrdenModal({
  open,
  compra,
  guardando,
  onClose,
  onConfirmar,
}) {
  const [lineas, setLineas] = useState([]);
  const [actualizarCostos, setActualizarCostos] = useState(true);
  const [errorLocal, setErrorLocal] = useState('');
  const [impactos, setImpactos] = useState(null);

  useEffect(() => {
    if (open && compra) {
      setLineas((compra.detalles || [])
        .filter((det) => (det.cantidadPendiente ?? 0) > 0 || det.cantidadPendiente == null)
        .map(lineaVacia)
        .filter((item) => item.pendiente > 0));
      setActualizarCostos(true);
      setErrorLocal('');
      setImpactos(null);
    }
  }, [open, compra]);

  const totalRecibir = useMemo(
    () => lineas.reduce((sum, item) => sum + Number(item.cantidadRecibida || 0), 0),
    [lineas],
  );

  function actualizarLinea(idx, campo, valor) {
    setLineas((actual) => actual.map((item, i) => (i === idx ? { ...item, [campo]: valor } : item)));
  }

  function recibirTodo() {
    setLineas((actual) => actual.map((item) => ({
      ...item,
      cantidadRecibida: item.pendiente,
      cantidadRechazada: 0,
    })));
  }

  async function enviar(event) {
    event.preventDefault();
    setErrorLocal('');
    const payload = {
      actualizarCostos,
      lineas: lineas.map((item) => ({
        detalleId: item.detalleId,
        cantidadRecibida: Number(item.cantidadRecibida) || 0,
        cantidadRechazada: Number(item.cantidadRechazada) || 0,
        notasQc: item.notasQc?.trim() || undefined,
      })).filter((item) => item.cantidadRecibida + item.cantidadRechazada > 0),
    };
    if (!payload.lineas.length) {
      setErrorLocal('Indique al menos una cantidad recibida o rechazada.');
      return;
    }
    for (const item of payload.lineas) {
      const origen = lineas.find((l) => l.detalleId === item.detalleId);
      const pendiente = origen?.pendiente ?? 0;
      if (item.cantidadRecibida + item.cantidadRechazada > pendiente) {
        setErrorLocal('Recibido + rechazado no puede superar el saldo pendiente.');
        return;
      }
    }
    try {
      const resultado = await onConfirmar(compra.id, payload);
      if (resultado?.impactosRecepcion?.length) {
        setImpactos(resultado.impactosRecepcion);
      } else {
        onClose();
      }
    } catch (err) {
      setErrorLocal(err?.message || 'No se pudo registrar la recepción.');
    }
  }

  const detalleMap = useMemo(() => {
    const map = new Map();
    (compra?.detalles || []).forEach((det) => map.set(det.id, det));
    return map;
  }, [compra]);

  return (
    <Modal
      open={open && Boolean(compra)}
      title={`Recibir ${compra?.numero || 'orden'}`}
      subtitle={compra?.proveedorNombre}
      onClose={onClose}
      size="lg"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>
            {impactos ? 'Cerrar' : 'Cancelar'}
          </Button>
          {!impactos ? (
            <>
              <Button type="button" variant="secondary" onClick={recibirTodo} disabled={guardando}>
                Recibir todo el saldo
              </Button>
              <Button type="submit" form="recibir-orden-form" disabled={guardando || totalRecibir <= 0}>
                {guardando ? 'Procesando…' : 'Confirmar recepción'}
              </Button>
            </>
          ) : null}
        </>
      )}
    >
      {impactos ? (
        <ImpactoRecepcionPanel impactos={impactos} />
      ) : (
        <form id="recibir-orden-form" onSubmit={enviar} className="com-recibir-orden-form">
          <label className="checkbox-inline">
            <input
              type="checkbox"
              checked={actualizarCostos}
              onChange={(e) => setActualizarCostos(e.target.checked)}
            />
            Actualizar costo ponderado y evaluar políticas de precio
          </label>
          {errorLocal ? <p className="form-error">{errorLocal}</p> : null}
          <div className="com-recibir-lineas">
            {lineas.map((linea, idx) => {
              const det = detalleMap.get(linea.detalleId);
              if (!det) return null;
              return (
                <article key={linea.detalleId} className="com-recibir-linea-card">
                  <header>
                    <strong>{det.productoNombre}</strong>
                    <small>{det.presentacionNombre} · Pendiente: {linea.pendiente} · {dinero(det.costoUnitario)}/u</small>
                  </header>
                  <div className="com-recibir-linea-grid">
                    <label>
                      Recibido
                      <input
                        type="number"
                        min="0"
                        max={linea.pendiente}
                        value={linea.cantidadRecibida}
                        onChange={(e) => actualizarLinea(idx, 'cantidadRecibida', e.target.value)}
                      />
                    </label>
                    <label>
                      Rechazado (QC)
                      <input
                        type="number"
                        min="0"
                        max={linea.pendiente}
                        value={linea.cantidadRechazada}
                        onChange={(e) => actualizarLinea(idx, 'cantidadRechazada', e.target.value)}
                      />
                    </label>
                    <label>
                      Notas QC
                      <input
                        type="text"
                        placeholder="Daño, vencimiento, faltante…"
                        value={linea.notasQc}
                        onChange={(e) => actualizarLinea(idx, 'notasQc', e.target.value)}
                      />
                    </label>
                  </div>
                </article>
              );
            })}
          </div>
          <p className="field-hint">
            <Icon name="info" size={14} strokeWidth={2} />
            {' '}
            Al confirmar: stock (solo recibido), CPP, catálogo proveedor y precio POS según política.
          </p>
        </form>
      )}
    </Modal>
  );
}
