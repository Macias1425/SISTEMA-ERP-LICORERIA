import { fecha, numero } from '../../utils/formato';

const ESTADOS = {
  OK: { texto: 'Rango vigente', clase: 'ok' },
  POR_AGOTARSE: { texto: 'Por agotarse', clase: 'warn' },
  AGOTADO: { texto: 'Rango agotado', clase: 'danger' },
  VENCIDO: { texto: 'Autorización vencida', clase: 'danger' },
  NO_CONFIGURADO: { texto: 'Sin configurar', clase: 'warn' },
  DESACTIVADA: { texto: 'Desactivada', clase: 'neutral' },
};

function EstadoFiscal({ estado }) {
  if (!estado) return null;
  const clasificacion = ESTADOS[estado.estado] || ESTADOS.NO_CONFIGURADO;

  return (
    <div className={`cfg-fiscal-estado ${clasificacion.clase}`}>
      <div className="cfg-fiscal-estado-head">
        <span className="cfg-fiscal-pill">{clasificacion.texto}</span>
        <p>{estado.mensaje}</p>
      </div>
      <dl className="cfg-fiscal-datos">
        <div>
          <dt>Próximo documento</dt>
          <dd>{estado.proximoNumero || '—'}</dd>
        </div>
        <div>
          <dt>Disponibles</dt>
          <dd>{numero(estado.documentosDisponibles)}</dd>
        </div>
        <div>
          <dt>Consumido</dt>
          <dd>{estado.porcentajeConsumido ?? 0}%</dd>
        </div>
        <div>
          <dt>Vence</dt>
          <dd>
            {fecha(estado.fechaLimiteEmision)}
            {estado.diasParaVencer !== null && estado.diasParaVencer !== undefined
              ? ` (${estado.diasParaVencer} d)`
              : ''}
          </dd>
        </div>
      </dl>
      <div className="cfg-fiscal-barra" aria-hidden="true">
        <span style={{ width: `${Math.min(100, estado.porcentajeConsumido ?? 0)}%` }} />
      </div>
    </div>
  );
}

/**
 * Régimen de facturación autorizado por la DGI: rango, correlativo y vigencia.
 * Con el régimen apagado el POS sigue vendiendo con su numeración interna.
 */
export default function FiscalSection({ form, onCampo }) {
  const habilitada = Boolean(form.facturacionFiscalHabilitada);

  return (
    <article className="card cfg-section-card">
      <h3>Facturación fiscal</h3>
      <p className="hint">
        Datos de la autorización de la DGI. Al activarlo, cada factura toma el siguiente correlativo del rango.
      </p>

      <EstadoFiscal estado={form.estadoFiscal} />

      <label className="check cfg-check-card">
        <input
          name="facturacionFiscalHabilitada"
          type="checkbox"
          checked={habilitada}
          onChange={onCampo}
        />
        <span>
          <strong>Emitir con numeración fiscal</strong>
          <small>Si se desactiva, las facturas conservan solo el número interno del POS.</small>
        </span>
      </label>

      <fieldset className="cfg-fiscal-campos" disabled={!habilitada}>
        <div className="form-grid">
          <label>
            RUC del emisor
            <input
              name="rucEmisor"
              value={form.rucEmisor || ''}
              onChange={onCampo}
              maxLength={20}
              placeholder="J0310000000000"
              required={habilitada}
            />
          </label>
          <label>
            Autorización de la DGI
            <input
              name="autorizacionDgi"
              value={form.autorizacionDgi || ''}
              onChange={onCampo}
              maxLength={60}
              placeholder="Número de resolución de autorización"
              required={habilitada}
            />
          </label>
          <label>
            Establecimiento
            <input
              name="establecimiento"
              value={form.establecimiento || ''}
              onChange={onCampo}
              maxLength={3}
              placeholder="000"
            />
          </label>
          <label>
            Punto de emisión
            <input
              name="puntoEmision"
              value={form.puntoEmision || ''}
              onChange={onCampo}
              maxLength={3}
              placeholder="001"
            />
          </label>
          <label>
            Tipo de documento
            <input
              name="tipoDocumentoFiscal"
              value={form.tipoDocumentoFiscal || ''}
              onChange={onCampo}
              maxLength={2}
              placeholder="01"
            />
          </label>
          <label>
            Rango inicial
            <input
              name="rangoInicial"
              type="number"
              min="1"
              value={form.rangoInicial ?? ''}
              onChange={onCampo}
              required={habilitada}
            />
          </label>
          <label>
            Rango final
            <input
              name="rangoFinal"
              type="number"
              min="1"
              value={form.rangoFinal ?? ''}
              onChange={onCampo}
              required={habilitada}
            />
          </label>
          <label>
            Correlativo actual
            <input
              name="correlativoActual"
              type="number"
              min="0"
              value={form.correlativoActual ?? ''}
              onChange={onCampo}
            />
            <small className="hint">Último número emitido. Déjelo vacío para empezar en el rango inicial.</small>
          </label>
          <label>
            Fecha límite de emisión
            <input
              name="fechaLimiteEmision"
              type="date"
              value={form.fechaLimiteEmision || ''}
              onChange={onCampo}
              required={habilitada}
            />
          </label>
        </div>
      </fieldset>
    </article>
  );
}
