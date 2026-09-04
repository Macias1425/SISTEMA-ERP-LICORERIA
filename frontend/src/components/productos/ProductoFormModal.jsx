import { useEffect, useState } from 'react';
import Button from '../ui/Button';
import Modal from '../ui/Modal';
import ProductoImagen from './ProductoImagen';
import { dinero } from '../../utils/formato';
import {
  MARGEN_DEFECTO,
  POLITICA_OPCIONES,
  POLITICA_PRECIO,
  margenEfectivo,
  precioVentaDesdeMargen,
} from '../../utils/politicaPrecio';

const vacio = {
  codigo: '',
  nombre: '',
  marca: '',
  urlImagen: '',
  categoriaId: '',
  precioCompra: '',
  precioVenta: '',
  politicaPrecio: POLITICA_PRECIO.MANUAL,
  margenObjetivoPct: '',
  stockActual: '0',
  stockMinimo: '0',
  stockCritico: '0',
  fechaVencimiento: '',
  esAlcoholico: true,
  activo: true,
};

export default function ProductoFormModal({
  open,
  producto,
  categorias,
  guardando,
  errorApi,
  onClose,
  onConfirmar,
}) {
  const [form, setForm] = useState(vacio);
  const [errorLocal, setErrorLocal] = useState('');

  useEffect(() => {
    if (!open) {
      setForm(vacio);
      setErrorLocal('');
      return;
    }
    if (producto) {
      const politica = producto.politicaPrecio || POLITICA_PRECIO.MANUAL;
      const margen = producto.margenObjetivoPct ?? '';
      const compra = producto.precioCompra ?? '';
      const ventaAuto = politica === POLITICA_PRECIO.AUTOMATICO_MARKUP
        ? precioVentaDesdeMargen(compra, margenEfectivo(producto, margen === '' ? null : margen))
        : null;
      setForm({
        codigo: producto.codigo ?? '',
        nombre: producto.nombre ?? '',
        marca: producto.marca || '',
        urlImagen: producto.urlImagen || '',
        categoriaId: producto.categoriaId ?? '',
        precioCompra: compra,
        precioVenta: ventaAuto ?? producto.precioVenta ?? '',
        politicaPrecio: politica,
        margenObjetivoPct: margen,
        stockActual: producto.stockActual ?? 0,
        stockMinimo: producto.stockMinimo ?? 0,
        stockCritico: producto.stockCritico ?? 0,
        fechaVencimiento: producto.fechaVencimiento || '',
        esAlcoholico: Boolean(producto.esAlcoholico),
        activo: Boolean(producto.activo),
      });
    } else {
      setForm(vacio);
    }
  }, [open, producto]);

  function ventaAutomatica(politica, precioCompra, margenObjetivoPct) {
    if (politica !== POLITICA_PRECIO.AUTOMATICO_MARKUP) return null;
    return precioVentaDesdeMargen(
      precioCompra,
      margenEfectivo(null, margenObjetivoPct === '' ? null : margenObjetivoPct),
    );
  }

  function onChange(event) {
    const { name, value, type, checked } = event.target;
    setForm((actual) => {
      const next = { ...actual, [name]: type === 'checkbox' ? checked : value };
      const politica = name === 'politicaPrecio' ? value : next.politicaPrecio;
      const compra = name === 'precioCompra' ? value : next.precioCompra;
      const margen = name === 'margenObjetivoPct' ? value : next.margenObjetivoPct;
      const ventaAuto = ventaAutomatica(politica, compra, margen);
      if (ventaAuto != null) {
        next.precioVenta = ventaAuto;
      }
      return next;
    });
  }

  function submit(event) {
    event.preventDefault();
    setErrorLocal('');
    const compra = Number(form.precioCompra);
    const venta = Number(form.precioVenta);
    const minimo = Number(form.stockMinimo || 0);
    const critico = Number(form.stockCritico || 0);
    const hoy = new Date().toISOString().slice(0, 10);

    if (!Number.isFinite(compra) || !Number.isFinite(venta)) {
      setErrorLocal('Indique precios de compra y venta válidos.');
      return;
    }
    if (venta < compra && form.politicaPrecio !== POLITICA_PRECIO.AUTOMATICO_MARKUP) {
      setErrorLocal('El precio de venta debe ser mayor o igual al precio de compra.');
      return;
    }
    const ventaFinal = form.politicaPrecio === POLITICA_PRECIO.AUTOMATICO_MARKUP
      ? (ventaAutomatica(form.politicaPrecio, compra, form.margenObjetivoPct) ?? venta)
      : venta;
    const margen = form.margenObjetivoPct === '' ? null : Number(form.margenObjetivoPct);
    if (margen != null && (!Number.isFinite(margen) || margen < 0 || margen >= 100)) {
      setErrorLocal('El margen objetivo debe estar entre 0 y 99.99 %.');
      return;
    }
    if (critico > minimo && minimo > 0) {
      setErrorLocal('El stock crítico no debe superar el stock mínimo.');
      return;
    }
    if (form.fechaVencimiento && form.fechaVencimiento < hoy && form.fechaVencimiento !== (producto?.fechaVencimiento || '')) {
      setErrorLocal('La fecha de vencimiento no puede ser anterior a hoy.');
      return;
    }
    onConfirmar?.({
      codigo: form.codigo.trim(),
      nombre: form.nombre.trim(),
      marca: form.marca.trim() || null,
      urlImagen: form.urlImagen.trim() || null,
      categoriaId: form.categoriaId === '' ? null : Number(form.categoriaId),
      precioCompra: compra,
      precioVenta: ventaFinal,
      politicaPrecio: form.politicaPrecio,
      margenObjetivoPct: margen,
      stockActual: producto ? undefined : Number(form.stockActual || 0),
      stockMinimo: minimo,
      stockCritico: critico,
      fechaVencimiento: form.fechaVencimiento || null,
      esAlcoholico: Boolean(form.esAlcoholico),
      activo: Boolean(form.activo),
    });
  }

  const categoriasActivas = categorias.filter((c) => c.activo || Number(c.id) === Number(form.categoriaId));
  const errorVisible = errorLocal || errorApi;
  const politicaSel = POLITICA_OPCIONES.find((item) => item.value === form.politicaPrecio);
  const usaMargen = form.politicaPrecio !== POLITICA_PRECIO.MANUAL;
  const precioSugeridoPreview = precioVentaDesdeMargen(
    form.precioCompra,
    margenEfectivo(null, form.margenObjetivoPct === '' ? null : form.margenObjetivoPct),
  );

  return (
    <Modal
      open={open}
      subtitle={producto ? 'Actualizar catálogo' : 'Alta de SKU'}
      title={producto ? `Editar ${producto.codigo}` : 'Nuevo producto'}
      onClose={onClose}
      size="lg"
      footer={(
        <>
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" form="form-producto" disabled={guardando}>
            {guardando ? 'Guardando…' : producto ? 'Actualizar' : 'Crear producto'}
          </Button>
        </>
      )}
    >
      <form id="form-producto" className="close-modal-form" onSubmit={submit}>
        {errorVisible ? <p className="pos-alert" role="alert">{errorVisible}</p> : null}
        <div className="form-grid">
          <label>
            Código
            <input name="codigo" value={form.codigo} onChange={onChange} required />
          </label>
          <label>
            Nombre
            <input name="nombre" value={form.nombre} onChange={onChange} required />
          </label>
          <label>
            Marca
            <input name="marca" value={form.marca} onChange={onChange} />
          </label>
          <label className="prod-form-img-field">
            URL de imagen
            <input
              name="urlImagen"
              type="text"
              value={form.urlImagen}
              onChange={onChange}
              placeholder="https://…"
            />
            <small className="field-hint">Enlace a foto del producto (opcional).</small>
          </label>
          <div className="prod-form-preview">
            <ProductoImagen
              producto={{
                urlImagen: form.urlImagen,
                esAlcoholico: form.esAlcoholico,
              }}
              size="lg"
            />
          </div>
          <label>
            Categoría
            <select name="categoriaId" value={form.categoriaId} onChange={onChange}>
              <option value="">Sin categoría</option>
              {categoriasActivas.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>
              ))}
            </select>
          </label>
          <label>
            Precio compra
            <input name="precioCompra" type="number" min="0" step="0.01" value={form.precioCompra} onChange={onChange} required />
          </label>
          <label>
            Precio venta
            <input
              name="precioVenta"
              type="number"
              min="0"
              step="0.01"
              value={form.precioVenta}
              onChange={onChange}
              readOnly={form.politicaPrecio === POLITICA_PRECIO.AUTOMATICO_MARKUP}
              required
            />
            {form.politicaPrecio === POLITICA_PRECIO.AUTOMATICO_MARKUP ? (
              <small className="field-hint">Se calcula y aplica solo al guardar y al recibir compras.</small>
            ) : null}
          </label>
          <label>
            Política de precio
            <select name="politicaPrecio" value={form.politicaPrecio} onChange={onChange}>
              {POLITICA_OPCIONES.map((opcion) => (
                <option key={opcion.value} value={opcion.value}>{opcion.label}</option>
              ))}
            </select>
            {politicaSel ? <small className="field-hint">{politicaSel.hint}</small> : null}
          </label>
          {usaMargen ? (
            <label>
              Margen objetivo (%)
              <input
                name="margenObjetivoPct"
                type="number"
                min="0"
                max="99.99"
                step="0.01"
                value={form.margenObjetivoPct}
                onChange={onChange}
                placeholder={String(MARGEN_DEFECTO)}
              />
              <small className="field-hint">
                Opcional. Si lo deja vacío el sistema usa {MARGEN_DEFECTO} %.
                {' '}Se aplica sobre el costo ponderado resultante de la compra, no sobre el costo de la factura suelta.
                {' '}Venta estimada con costo actual:{' '}
                {precioSugeridoPreview != null ? dinero(precioSugeridoPreview) : '—'}
              </small>
            </label>
          ) : null}
          {!producto ? (
            <label>
              Stock inicial (UMM)
              <input name="stockActual" type="number" min="0" value={form.stockActual} onChange={onChange} />
            </label>
          ) : null}
          <label>
            Stock mínimo
            <input name="stockMinimo" type="number" min="0" value={form.stockMinimo} onChange={onChange} />
          </label>
          <label>
            Stock crítico
            <input name="stockCritico" type="number" min="0" value={form.stockCritico} onChange={onChange} />
          </label>
          <label>
            Fecha vencimiento
            <input name="fechaVencimiento" type="date" value={form.fechaVencimiento} onChange={onChange} />
          </label>
          <label className="check">
            <input type="checkbox" name="esAlcoholico" checked={form.esAlcoholico} onChange={onChange} />
            Alcohólico
          </label>
          <label className="check">
            <input type="checkbox" name="activo" checked={form.activo} onChange={onChange} />
            Activo en catálogo
          </label>
        </div>
      </form>
    </Modal>
  );
}
