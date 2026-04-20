# Local Docker Secrets

Create these files before running `docker compose up --build`:

- `secrets/local/auth_db_password`
- `secrets/local/booking_db_password`
- `secrets/local/driver_db_password`
- `secrets/local/rating_db_password`
- `secrets/local/analytics_db_password`
- `secrets/local/fraud_db_password`
- `secrets/local/jwt_secret`

Each file should contain only the raw secret value with no surrounding quotes.

For deployment environments, mount your external secret manager output into a directory like `/mnt/secrets/porterlike/` and set `APP_SECRET_PATH` to that directory.