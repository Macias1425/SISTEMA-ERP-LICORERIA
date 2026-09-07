import Button from '../ui/Button';
import Icon from '../ui/Icon';
import { hoyLocalIso } from '../../utils/edadUtil';
import { dinero } from '../../utils/formato';
import StripePaymentBox from './StripePaymentBox';

const DENOMINACIONES = [10, 20, 50, 100, 200, 500];

export default function PaymentPanel({
  clientes,
  clienteId,
  tipoCliente,
  tipoClienteAplicado,
  avisos = [],
  cotizando = false,
  bloqueado = false,
  formaPago,
  hayAlcohol,
  confirmaEdad,
  fechaNacimiento,
  edadMinima,
  verificacionEdad = { ok: true },
  licorPermitido = true,
  ummTicket,
  volumenMinimo,
  subtotal = 0,
  impuesto = 0,
  tasaIva = 0.15,
  total,
  montoRecibido,
  exigeSupervisor = false,
  limiteVentasAlcanzado = false,
  cobrando,
  stripeEnabled = false,
  stripePublishableKey = '',
  stripeCurrency = 'usd',
  clienteCreditoOk = false,
  onChange,
  onCobrar,
  onCancelar,
  onFormaPago,
  onMontoExacto,
  onAgregarEfectivo,
  onStripePaid,
}) {
  const minimoMayorista = Number(volumenMinimo || 6);
  const totalCobrar = Number(total || 0);
  const recibido = Number(montoRecibido || 0);
  const efectivo = formaPago === 'EFECTIVO';
  const stripe = formaPago === 'STRIPE';
  const credito = formaPago === 'CREDITO';
  const faltaEfectivo = efectivo && (montoRecibido === '' || recibido + 1e-9 < totalCobrar);
  const porPagar = efectivo ? Math.max(0, totalCobrar - recibido) : 0;
  const vuelto = efectivo && !faltaEfectivo ? recibido - totalCobrar : 0;
  const horarioBloqueado = hayAlcohol && !licorPermitido;
  const edadOk = !hayAlcohol || verificacionEdad.ok;
  const baseOk = !cobrando && !cotizando && !bloqueado && totalCobrar > 0
    && edadOk && !horarioBloqueado && !limiteVentasAlcanzado;
  const puedeCobrar = baseOk && !faltaEfectivo && !stripe;
  const stripeBloqueado = !baseOk;
  const hoy = hoyLocalIso();

  return (
    <section className="pos-checkout" aria-label="Cobro">
      <div className="pos-checkout-body">
        <h3 className="pos-checkout-title">Cobro</h3>
        {hayAlcohol ? (
          <div className={`pos-checkout-age${edadOk ? '' : ' is-invalid'}`}>
            <h3>Verificación de edad</h3>
            {!edadOk ? (
              <p className="pos-checkout-age-error" role="alert">{verificacionEdad.mensaje}</p>
            ) : (
              <p className="pos-checkout-age-hint">Confirme visualmente o ingrese fecha de nacimiento.</p>
            )}
            <label className="pos-check">
              <input
                type="checkbox"
                name="confirmaEdad"
                checked={confirmaEdad}
                onChange={onChange}
              />
              <span>Cliente mayor de {edadMinima} años (confirmación visual)</span>
            </label>
            <p className="pos-checkout-age-or">o</p>
            <label className="pos-checkout-field">
              <span>Fecha de nacimiento</span>
              <input
                name="fechaNacimiento"
                type="date"
                max={hoy}
                value={fechaNacimiento}
                onChange={onChange}
              />
            </label>
          </div>
        ) : null}

        {horarioBloqueado ? (
          <p className="pos-checkout-alert" role="alert">
            No se puede vender licor en este horario.
          </p>
        ) : null}
        {limiteVentasAlcanzado ? (
          <p className="pos-checkout-alert" role="alert">
            Límite de ventas del turno alcanzado.
          </p>
        ) : null}
        {exigeSupervisor ? (
          <p className="pos-checkout-note">Venta alta: se pedirá clave de administrador.</p>
        ) : null}

        <label className="pos-checkout-client">
          <Icon name="user" size={18} strokeWidth={2} />
          <select name="clienteId" value={clienteId} onChange={onChange} aria-label="Asociar cliente">
            {clientes.map((cliente) => (
              <option key={cliente.id} value={cliente.id}>{cliente.nombre}</option>
            ))}
          </select>
        </label>

        <label className="pos-checkout-type">
          <span>Tipo de precio</span>
          <select name="tipoCliente" value={tipoCliente} onChange={onChange}>
            <option value="DETAL">Detalle</option>
            <option value="MAYORISTA">Mayorista</option>
          </select>
        </label>
        {tipoCliente === 'MAYORISTA' && ummTicket < minimoMayorista ? (
          <p className="pos-checkout-note warn">
            Mayorista requiere mínimo {minimoMayorista} uds. Lleva {ummTicket}.
          </p>
        ) : null}
        {tipoClienteAplicado && tipoClienteAplicado !== tipoCliente ? (
          <p className="pos-checkout-note warn">
            El servidor aplicará tarifa {tipoClienteAplicado === 'DETAL' ? 'detalle' : 'mayorista'}.
          </p>
        ) : null}
        {avisos.map((aviso) => (
          <p key={aviso} className="pos-checkout-note warn">{aviso}</p>
        ))}

        <div className="pos-checkout-summary">
          <div className="pos-checkout-summary-row">
            <span>Subtotal</span>
            <span>{dinero(subtotal)}</span>
          </div>
          <div className="pos-checkout-summary-total">
            <span>Total a cobrar</span>
            <strong>{dinero(totalCobrar)}</strong>
          </div>
          {cotizando ? <p className="pos-checkout-tax">Confirmando precios con el servidor…</p> : null}
          <p className="pos-checkout-tax">
            IVA {Math.round(Number(tasaIva || 0.15) * 100)}% · {dinero(impuesto)}
          </p>
        </div>

        <div className="pos-checkout-methods" role="tablist" aria-label="Forma de pago">
          <button
            type="button"
            role="tab"
            aria-selected={formaPago === 'EFECTIVO'}
            className={`pos-checkout-method${formaPago === 'EFECTIVO' ? ' active' : ''}`}
            onClick={() => onFormaPago('EFECTIVO')}
          >
            <Icon name="banknote" size={16} strokeWidth={2} />
            Efectivo
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={formaPago === 'TARJETA'}
            className={`pos-checkout-method${formaPago === 'TARJETA' ? ' active' : ''}`}
            onClick={() => onFormaPago('TARJETA')}
          >
            <Icon name="creditCard" size={16} strokeWidth={2} />
            Tarjeta
          </button>
          {stripeEnabled ? (
            <button
              type="button"
              role="tab"
              aria-selected={formaPago === 'STRIPE'}
              className={`pos-checkout-method${formaPago === 'STRIPE' ? ' active' : ''}`}
              onClick={() => onFormaPago('STRIPE')}
            >
              <Icon name="wallet" size={16} strokeWidth={2} />
              Stripe
            </button>
          ) : null}
          {clienteCreditoOk ? (
            <button
              type="button"
              role="tab"
              aria-selected={formaPago === 'CREDITO'}
              className={`pos-checkout-method${formaPago === 'CREDITO' ? ' active' : ''}`}
              onClick={() => onFormaPago('CREDITO')}
            >
              <Icon name="hand" size={16} strokeWidth={2} />
              Crédito
            </button>
          ) : null}
        </div>

        {efectivo ? (
          <div className="pos-checkout-cash">
            <label className="pos-checkout-field">
              <span>Monto recibido</span>
              <input
                name="montoRecibido"
                type="number"
                min="0"
                step="0.01"
                inputMode="decimal"
                placeholder="0.00"
                value={montoRecibido}
                onChange={onChange}
              />
            </label>
            <button type="button" className="pos-checkout-exact" onClick={onMontoExacto} disabled={totalCobrar <= 0}>
              Monto exacto
            </button>
            <div className={`pos-checkout-due${faltaEfectivo && recibido > 0 ? ' is-short' : ''}`}>
              <span>{faltaEfectivo ? 'Por pagar' : 'Vuelto'}</span>
              <strong>{dinero(faltaEfectivo ? porPagar : Math.max(0, vuelto))}</strong>
            </div>
            <div className="pos-checkout-quick">
              {DENOMINACIONES.map((monto) => (
                <button
                  key={monto}
                  type="button"
                  className="pos-checkout-quick-btn"
                  onClick={() => onAgregarEfectivo(monto)}
                >
                  +{monto}
                </button>
              ))}
            </div>
          </div>
        ) : stripe ? (
          <StripePaymentBox
            total={totalCobrar}
            publishableKey={stripePublishableKey}
            currency={stripeCurrency}
            disabled={stripeBloqueado}
            onPaid={onStripePaid}
            onCancel={onCancelar}
          />
        ) : credito ? (
          <p className="pos-checkout-card-hint">
            Se cargará al saldo del cliente. Debe tener límite de crédito disponible.
          </p>
        ) : (
          <p className="pos-checkout-card-hint">El cobro con tarjeta registrará el monto total.</p>
        )}
      </div>

      {!stripe ? (
        <footer className="pos-checkout-foot">
          <button
            type="button"
            className="pos-checkout-pay"
            onClick={onCobrar}
            disabled={!puedeCobrar}
          >
            <Icon name="wallet" size={20} strokeWidth={2} />
            {cobrando ? 'Procesando…' : `Cobrar ${dinero(totalCobrar)}`}
          </button>
          <Button type="button" variant="secondary" className="pos-checkout-cancel" onClick={onCancelar} disabled={cobrando}>
            <Icon name="x" size={16} strokeWidth={2.5} />
            Cancelar
          </Button>
        </footer>
      ) : null}
    </section>
  );
}
