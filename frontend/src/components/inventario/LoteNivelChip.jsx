const ETIQUETAS = {
  VENCIDO: { texto: 'Vencido', clase: 'inv-chip-critico' },
  POR_VENCER: { texto: 'Por vencer', clase: 'inv-chip-minimo' },
  VIGENTE: { texto: 'Vigente', clase: 'inv-chip-ok' },
  SIN_FECHA: { texto: 'Sin fecha', clase: '' },
};

export default function LoteNivelChip({ nivel }) {
  const { texto, clase } = ETIQUETAS[nivel] || ETIQUETAS.SIN_FECHA;
  return <span className={`inv-chip ${clase}`.trim()}>{texto}</span>;
}
