import { ahoraDocumento, periodoTexto } from '../../utils/exportCsv';

export default function ReportePrintHeader({
  titulo,
  subtitulo,
  tipo = 'Reporte interno',
  negocio = 'Sistema POS Licorería',
  desde,
  hasta,
  periodo,
  generadoEn,
  registros,
  filtros,
}) {
  const rango = periodo || periodoTexto(desde, hasta);
  return (
    <header className="rep-print-header">
      <p className="rep-print-kicker">{negocio}</p>
      <p className="rep-print-tipo">{tipo}</p>
      <h2>{titulo}</h2>
      {subtitulo ? <p className="rep-print-sub">{subtitulo}</p> : null}
      <dl className="rep-print-meta-grid">
        {rango ? (
          <>
            <dt>Período consultado</dt>
            <dd>{rango}</dd>
          </>
        ) : null}
        <dt>Fecha de emisión</dt>
        <dd>{generadoEn || ahoraDocumento()}</dd>
        {registros != null ? (
          <>
            <dt>Registros incluidos</dt>
            <dd>{registros}</dd>
          </>
        ) : null}
        {filtros ? (
          <>
            <dt>Filtros aplicados</dt>
            <dd>{filtros}</dd>
          </>
        ) : null}
      </dl>
      <p className="rep-print-legal">
        Documento generado por el sistema. Los montos están expresados en córdobas (C$).
        Destinado a control interno; no sustituye la factura fiscal.
      </p>
    </header>
  );
}
