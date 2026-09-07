# Docker + entorno local

## Levantar todo

```bash
cp .env.example .env
docker compose up --build
```

- App: http://localhost:5173  
- API: http://localhost:8080  
- Login: `admin` / `admin123`  
- MySQL del compose usa `MYSQL_ROOT_PASSWORD` (por defecto `posroot`). No hace falta MySQL instalado en la laptop.

## Sin Docker (como antes)

Backend y frontend locales con tu MySQL. La contraseña sigue en `DB_PASSWORD` / `application.yml`.

## Stripe (opcional)

Por defecto **apagado**: efectivo y tarjeta manual no cambian.

1. En `.env`: `STRIPE_ENABLED=true` y claves de prueba (`sk_test_…`, `pk_test_…`).
2. `docker compose up --build` de nuevo (o reiniciar el backend).
3. En el POS aparece el método **Stripe**. Tarjeta de prueba: `4242 4242 4242 4242`.

Sin claves o con `STRIPE_ENABLED=false`, el botón Stripe no se muestra.
