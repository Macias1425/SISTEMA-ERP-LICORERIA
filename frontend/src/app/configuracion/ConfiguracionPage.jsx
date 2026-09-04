import { useEffect, useMemo, useRef, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import ConfigResumenCards, { textoEstadoHorarios } from '../../components/configuracion/ConfigResumenCards';
import ConfigSidebar, { SECCIONES } from '../../components/configuracion/ConfigSidebar';
import FiscalSection from '../../components/configuracion/FiscalSection';
import HorarioEditor, { formatearActualizado, horaInput } from '../../components/configuracion/HorarioEditor';
import Button from '../../components/ui/Button';
import { configuracionService } from '../../services/configuracionService';

function normalizarForm(data) {
  return {
    ...data,
    tasaIvaPct: Number(data.tasaIva ?? 0) * 100,
    horarios: (data.horarios || []).map((horario) => ({
      ...horario,
      horaInicio: horaInput(horario.horaInicio),
      horaFin: horaInput(horario.horaFin),
    })),
  };
}

/** Campos numéricos opcionales del régimen fiscal: vacío significa "sin definir", no cero. */
function numeroOpcional(valor) {
  return valor === '' || valor === null || valor === undefined ? null : Number(valor);
}

function textoOpcional(valor) {
  const limpio = String(valor ?? '').trim();
  return limpio === '' ? null : limpio;
}

function payloadDesdeForm(form) {
  return {
    nombreNegocio: String(form.nombreNegocio || '').trim(),
    direccionNegocio: textoOpcional(form.direccionNegocio),
    telefonoNegocio: textoOpcional(form.telefonoNegocio),
    edadMinimaAlcohol: Number(form.edadMinimaAlcohol),
    horarioHabilitado: Boolean(form.horarioHabilitado),
    tasaIva: Number(form.tasaIvaPct) / 100,
    volumenMinimoUmm: Number(form.volumenMinimoUmm),
    requiereTurnoAbiertoParaAnular: Boolean(form.requiereTurnoAbiertoParaAnular),
    maxIntentosLogin: Number(form.maxIntentosLogin),
    bloqueoMinutos: Number(form.bloqueoMinutos),
    horarios: (form.horarios || []).map((horario) => ({
      id: horario.id,
      diaSemana: horario.diaSemana,
      horaInicio: horario.horaInicio,
      horaFin: horario.horaFin,
      activo: Boolean(horario.activo),
    })),
    facturacionFiscalHabilitada: Boolean(form.facturacionFiscalHabilitada),
    rucEmisor: textoOpcional(form.rucEmisor),
    autorizacionDgi: textoOpcional(form.autorizacionDgi),
    establecimiento: textoOpcional(form.establecimiento),
    puntoEmision: textoOpcional(form.puntoEmision),
    tipoDocumentoFiscal: textoOpcional(form.tipoDocumentoFiscal),
    rangoInicial: numeroOpcional(form.rangoInicial),
    rangoFinal: numeroOpcional(form.rangoFinal),
    correlativoActual: numeroOpcional(form.correlativoActual),
    fechaLimiteEmision: textoOpcional(form.fechaLimiteEmision),
  };
}

function firmaPayload(payload) {
  return JSON.stringify(payload);
}

export default function ConfiguracionPage() {
  const [form, setForm] = useState(null);
  const [baseline, setBaseline] = useState('');
  const [seccion, setSeccion] = useState('resumen');
  const [busquedaSidebar, setBusquedaSidebar] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const importRef = useRef(null);

  async function cargar() {
    setCargando(true);
    setError('');
    try {
      const data = normalizarForm(await configuracionService.obtener());
      setForm(data);
      setBaseline(firmaPayload(payloadDesdeForm(data)));
      setOk('');
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  const pendiente = useMemo(() => {
    if (!form) return false;
    return firmaPayload(payloadDesdeForm(form)) !== baseline;
  }, [form, baseline]);

  function onCampo(event) {
    const { name, value, type, checked } = event.target;
    setForm((actual) => ({ ...actual, [name]: type === 'checkbox' ? checked : value }));
    setOk('');
  }

  async function onSubmit(event) {
    event?.preventDefault?.();
    if (!form || !pendiente) return;
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const guardada = normalizarForm(await configuracionService.guardar(payloadDesdeForm(form)));
      setForm(guardada);
      setBaseline(firmaPayload(payloadDesdeForm(guardada)));
      setOk('Parámetros globales guardados. Ya aplican a POS, facturas e inventario.');
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  function exportarJson() {
    if (!form) return;
    const blob = new Blob([JSON.stringify(payloadDesdeForm(form), null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'configuracion-pos.json';
    enlace.click();
    URL.revokeObjectURL(url);
  }

  function importarJson(event) {
    const archivo = event.target.files?.[0];
    if (!archivo) return;
    const lector = new FileReader();
    lector.onload = () => {
      try {
        const data = JSON.parse(String(lector.result || '{}'));
        setForm((actual) => normalizarForm({
          ...actual,
          ...data,
          horarios: data.horarios || actual?.horarios,
        }));
        setOk('');
        setError('');
      } catch {
        setError('El archivo JSON no es válido.');
      } finally {
        event.target.value = '';
      }
    };
    lector.readAsText(archivo);
  }

  return (
    <section className="config-page">
      <header className="cfg-topbar">
        <div>
          <p className="brand-kicker">Parámetros del sistema</p>
          <h1>Configuración global</h1>
          <p className="cfg-subtitle">
            Normativa de licor, impuestos, horarios, facturación y seguridad. Los cambios aplican a todo el POS.
          </p>
        </div>
        <div className="cfg-topbar-actions no-print">
          {pendiente ? <span className="cfg-pending-badge">Cambios sin guardar</span> : null}
          <button type="button" className="btn secondary" onClick={exportarJson} disabled={!form}>
            Exportar
          </button>
          <button type="button" className="btn secondary" onClick={() => importRef.current?.click()} disabled={!form}>
            Importar
          </button>
          <input ref={importRef} type="file" accept="application/json" hidden onChange={importarJson} />
          <button type="button" className="btn secondary" onClick={cargar} disabled={cargando}>
            Restablecer
          </button>
        </div>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      {cargando ? <p className="placeholder">Cargando parámetros…</p> : null}
      {!cargando && !form ? (
        <p className="placeholder">No se pudo cargar la configuración. Reinicie el backend.</p>
      ) : null}

      {!cargando && form ? (
        <div className="cfg-shell">
          <ConfigSidebar
            seccion={seccion}
            busqueda={busquedaSidebar}
            onBusqueda={setBusquedaSidebar}
            onSeccion={setSeccion}
          />

          <div className="cfg-main">
            <form id="form-config" className="cfg-form" onSubmit={onSubmit}>
              {seccion === 'resumen' ? (
                <>
                  <article className="card cfg-status-card">
                    <div className="cfg-status-head">
                      <div>
                        <h3>Estado de configuración</h3>
                        <p className="hint">
                          Última actualización: {formatearActualizado(form.actualizadoEn)}
                          {pendiente ? ' · Hay cambios pendientes de guardar' : ''}
                        </p>
                      </div>
                      <span className={`cfg-status-pill${pendiente ? ' warn' : ' ok'}`}>
                        {pendiente ? 'Pendiente' : 'Sincronizado'}
                      </span>
                    </div>
                    <div className="cfg-quick-actions">
                      {['empresa', 'normativa', 'horarios', 'fiscal', 'seguridad'].map((id) => (
                        <button key={id} type="button" className="cfg-quick-btn" onClick={() => setSeccion(id)}>
                          Editar {SECCIONES.find((s) => s.id === id)?.label || id}
                        </button>
                      ))}
                    </div>
                    <p className="hint cfg-status-foot">{textoEstadoHorarios(form)}</p>
                  </article>
                  <ConfigResumenCards form={form} onEditar={setSeccion} />
                </>
              ) : null}

              {seccion === 'empresa' ? (
                <article className="card cfg-section-card">
                  <h3>Empresa</h3>
                  <p className="hint">Identidad comercial y reglas de precio mayorista.</p>
                  <div className="form-grid">
                    <label>
                      Nombre comercial
                      <input
                        name="nombreNegocio"
                        value={form.nombreNegocio}
                        onChange={onCampo}
                        minLength={3}
                        maxLength={120}
                        required
                      />
                    </label>
                    <label>
                      Dirección
                      <input
                        name="direccionNegocio"
                        value={form.direccionNegocio || ''}
                        onChange={onCampo}
                        maxLength={180}
                        placeholder="Barrio, calle, ciudad"
                      />
                    </label>
                    <label>
                      Teléfono
                      <input
                        name="telefonoNegocio"
                        value={form.telefonoNegocio || ''}
                        onChange={onCampo}
                        maxLength={30}
                        placeholder="2234-5678"
                      />
                    </label>
                    <label>
                      Volumen mínimo mayorista (botellas UMM)
                      <input
                        name="volumenMinimoUmm"
                        type="number"
                        min="1"
                        max="9999"
                        value={form.volumenMinimoUmm}
                        onChange={onCampo}
                        required
                      />
                    </label>
                  </div>
                </article>
              ) : null}

              {seccion === 'normativa' ? (
                <article className="card cfg-section-card">
                  <h3>Normativa e impuestos</h3>
                  <div className="form-grid">
                    <label>
                      Edad mínima para alcohol
                      <input
                        name="edadMinimaAlcohol"
                        type="number"
                        min="18"
                        max="99"
                        value={form.edadMinimaAlcohol}
                        onChange={onCampo}
                        required
                      />
                    </label>
                    <label>
                      IVA (%)
                      <input
                        name="tasaIvaPct"
                        type="number"
                        min="0"
                        max="100"
                        step="0.01"
                        value={form.tasaIvaPct}
                        onChange={onCampo}
                        required
                      />
                    </label>
                    <label className="check cfg-check-card">
                      <input
                        name="horarioHabilitado"
                        type="checkbox"
                        checked={form.horarioHabilitado}
                        onChange={onCampo}
                      />
                      <span>
                        <strong>Aplicar horario de venta de licor</strong>
                        <small>Bloquea alcohol fuera del horario configurado.</small>
                      </span>
                    </label>
                    <label className="check cfg-check-card">
                      <input
                        name="requiereTurnoAbiertoParaAnular"
                        type="checkbox"
                        checked={form.requiereTurnoAbiertoParaAnular}
                        onChange={onCampo}
                      />
                      <span>
                        <strong>Exigir turno abierto para anular facturas</strong>
                        <small>Anulación solo con caja del mismo turno.</small>
                      </span>
                    </label>
                  </div>
                </article>
              ) : null}

              {seccion === 'horarios' ? (
                <article className="card cfg-section-card">
                  <h3>Horario semanal de licor</h3>
                  <HorarioEditor
                    horarios={form.horarios}
                    habilitado={form.horarioHabilitado}
                    onChange={(horarios) => {
                      setForm((actual) => ({ ...actual, horarios }));
                      setOk('');
                    }}
                  />
                </article>
              ) : null}

              {seccion === 'fiscal' ? <FiscalSection form={form} onCampo={onCampo} /> : null}

              {seccion === 'seguridad' ? (
                <article className="card cfg-section-card">
                  <h3>Seguridad de acceso</h3>
                  <p className="hint">Protección contra intentos fallidos de login.</p>
                  <div className="form-grid">
                    <label>
                      Intentos máximos de login
                      <input
                        name="maxIntentosLogin"
                        type="number"
                        min="3"
                        max="20"
                        value={form.maxIntentosLogin}
                        onChange={onCampo}
                        required
                      />
                    </label>
                    <label>
                      Minutos de bloqueo
                      <input
                        name="bloqueoMinutos"
                        type="number"
                        min="5"
                        max="120"
                        value={form.bloqueoMinutos}
                        onChange={onCampo}
                        required
                      />
                    </label>
                  </div>
                </article>
              ) : null}
            </form>
          </div>
        </div>
      ) : null}

      {!cargando && form ? (
        <footer className="cfg-savebar no-print">
          <p>{pendiente ? 'Tiene cambios sin guardar en parámetros globales.' : 'Todo sincronizado con el servidor.'}</p>
          <Button type="submit" form="form-config" disabled={guardando || !pendiente}>
            {guardando ? 'Guardando…' : 'Guardar cambios pendientes'}
          </Button>
        </footer>
      ) : null}
    </section>
  );
}
