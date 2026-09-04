import { useState } from 'react';
import Icon from '../ui/Icon';

export default function ProductoImagen({
  producto,
  size = 'md',
  className = '',
}) {
  const [error, setError] = useState(false);
  const url = producto?.urlImagen?.trim();
  const mostrarImagen = Boolean(url) && !error;
  const icono = producto?.esAlcoholico ? 'wine' : 'package';

  return (
    <div
      className={`prod-img prod-img-${size}${className ? ` ${className}` : ''}`}
      aria-hidden={mostrarImagen ? undefined : true}
    >
      {mostrarImagen ? (
        <img
          src={url}
          alt=""
          loading="lazy"
          onError={() => setError(true)}
        />
      ) : (
        <span className="prod-img-fallback">
          <Icon name={icono} size={size === 'sm' ? 20 : size === 'lg' ? 36 : 28} strokeWidth={1.75} />
        </span>
      )}
    </div>
  );
}
