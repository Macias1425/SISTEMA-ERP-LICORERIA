import { useEffect, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function PresentacionModal({
  open,
  producto,
  presentacion,
  guardando,
  errorApi,
  onClose,
  onConfirmar,
}) {
  const edicion = Boolean(presentacion?.id);
  const [nombre, setNombre] = useState('');
  const [factor, setFactor] = useState('1');
  const [activo, setActivo] = useState(true);
  const [errorLocal, setErrorLocal] = useState('');

  useEffect(() => {
    if (!open) return;
    setNombre(presentacion?.nombre || '');
    setFactor(String(presentacion?.factorAUnidadMinima ?? 1));
    setActivo(presentacion?.activo !== false);
    setErrorLocal('');
  }, [open, presentacion]);

  function submit(event) {
    event.preventDefault();
    setErrorLocal('');
    const factorNum = Number(factor);
    if (!nombre.trim()) {
      setErrorLocal('Indique el nombre de la presentación.');
      return;
    }
    if (!Number.isFinite(factorNum) || factorNum < 1) {
      setErrorLocal('El factor debe ser al menos 1 (ej. Caja = 12 botellas → factor 12).');
      return;
    }
    onConfirmar?.({
      id: presentacion?.id,
      nombre: nombre.trim(),
      factorAUnidadMinima: factorNum,
      activo,
    });
  }

  const errorVisible = errorLocal || errorApi;

  return (
    <Modal
      open={open}
      subtitle="Empaque / unidad de venta"
      title={`${edicion ? 'Editar presentación' : 'Presentación'} · ${producto?.nombre || ''}`}
      onClose={onClose}
      size="md"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-presentacion" disabled={guardando || !producto?.id}>
            {guardando ? 'Guardando…' : edicion ? 'Guardar cambios' : 'Agregar'}
          </Button>
        </>
      )}
    >
      <form id="form-presentacion" className="close-modal-form" onSubmit={submit}>
        {errorVisible ? <p className="pos-alert" role="alert">{errorVisible}</p> : null}
        <p className="close-modal-lead">
          El factor indica cuántas unidades mínimas (botellas) contiene la presentación. Ej: nombre «Caja», factor 12.
        </p>
        <div className="form-grid">
          <label>
            Nombre
            <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Botella, Caja, Six-pack…" required />
          </label>
          <label>
            Factor a UMM
            <input type="number" min="1" step="1" value={factor} onChange={(e) => setFactor(e.target.value)} required />
          </label>
        </div>
        {edicion ? (
          <label className="check">
            <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} />
            <span>Disponible para vender</span>
          </label>
        ) : null}
      </form>
    </Modal>
  );
}
