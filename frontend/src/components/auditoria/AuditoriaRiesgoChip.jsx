function claseRiesgo(nivel) {
  if (nivel === 'ALTO') return 'aud-riesgo aud-riesgo-alto';
  if (nivel === 'MEDIO') return 'aud-riesgo aud-riesgo-medio';
  return 'aud-riesgo aud-riesgo-bajo';
}

function etiquetaRiesgo(nivel) {
  if (nivel === 'ALTO') return 'Alto';
  if (nivel === 'MEDIO') return 'Medio';
  return 'Bajo';
}

function IconoAlerta() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M12 9v4M12 17h.01" />
      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
    </svg>
  );
}

export default function AuditoriaRiesgoChip({ nivel }) {
  return (
    <span className={claseRiesgo(nivel)}>
      {nivel === 'ALTO' ? <IconoAlerta /> : null}
      {etiquetaRiesgo(nivel)}
    </span>
  );
}
