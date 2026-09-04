import { useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';

export default function RestaurarModal({
  baseDatos,
  respaldoNombre,
  onCerrar,
  onConfirmar,
  procesando,
}) {
  const [archivo, setArchivo] = useState(null);
  const [confirmacion, setConfirmacion] = useState('');
  const existente = Boolean(respaldoNombre);
  const coincide = confirmacion.trim().toLowerCase() === String(baseDatos || '').toLowerCase();
  const habilitado = coincide && (existente || archivo);

  return (
    <Modal
      open
      size="md"
      subtitle="Operación crítica"
      title={existente ? 'Restaurar respaldo existente' : 'Restaurar base de datos SQL'}
      onClose={procesando ? undefined : onCerrar}
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onCerrar} disabled={procesando}>
            Cancelar
          </Button>
          <Button
            type="button"
            variant="danger"
            disabled={!habilitado || procesando}
            onClick={() => onConfirmar?.({ archivo, confirmacion, nombreExistente: respaldoNombre })}
          >
            {procesando ? 'Restaurando…' : 'Restaurar ahora'}
          </Button>
        </>
      )}
    >
      <div className="mnt-restore-alert">
        Esta acción sobrescribe los datos actuales. Antes de restaurar el sistema crea un respaldo de seguridad
        automático. Conserve también una copia fuera del servidor.
      </div>

      {existente ? (
        <p className="mnt-restore-file">
          Archivo a restaurar: <code>{respaldoNombre}</code>
        </p>
      ) : (
        <label className="mnt-restore-field">
          Archivo .sql
          <input
            type="file"
            accept=".sql,text/sql,application/sql"
            onChange={(e) => setArchivo(e.target.files?.[0] || null)}
          />
        </label>
      )}

      <label className="mnt-restore-field">
        Confirmación
        <span className="hint">Escriba exactamente: <strong>{baseDatos}</strong></span>
        <input
          type="text"
          value={confirmacion}
          onChange={(e) => setConfirmacion(e.target.value)}
          placeholder={baseDatos}
          autoComplete="off"
          disabled={procesando}
        />
      </label>
      <p className="hint">Solo administradores pueden restaurar. Quedará registrado en auditoría.</p>
    </Modal>
  );
}
