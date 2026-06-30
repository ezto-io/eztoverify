# ezto Socket

A real-time socket communication service built with Express.js and Socket.io. Manages WebSocket connections with transaction-based routing for status updates and message forwarding.

## Features

- Real-time WebSocket communication
- Transaction ID-based connection routing
- Status update broadcasting
- PostgreSQL-backed connection persistence
- Health check endpoint

## Tech Stack

- Node.js with Express
- Socket.io
- PostgreSQL (via `pg` driver)

## Prerequisites

- Node.js 18+
- PostgreSQL 14+

## Setup

1. Configure environment variables:
   ```bash
   cp .env .env.local
   # Edit .env.local with your database credentials
   ```

2. Install and run:
   ```bash
   ./start.sh
   ```
   `start.sh` installs deps, ensures the `socket` table exists
   (`db/schema.sql`, idempotent), then starts the server.
   Skip steps with `SKIP_INSTALL=1` / `SKIP_MIGRATE=1`.
   Manual: `npm install && node db/bootstrap.js && npm start`.

### Dev mode (against the shared dev infra)

Start the backing infra once from the repo root, then run this service on the host:

```bash
docker compose -f docker-compose.dev.yml up -d        # postgres/minio/es/maildev on localhost
cp .env.example .env                                  # then set the values below
```

`.env` for the dev Postgres (db `socket`):

```bash
PORT=3001
PGHOST=localhost
PGPORT=5432
PGUSER=admin
PGPASSWORD=admin
PGDATABASE=socket
```

```bash
./start.sh
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `PGUSER` | PostgreSQL username |
| `PGHOST` | PostgreSQL host |
| `PGDATABASE` | PostgreSQL database name |
| `PGPASSWORD` | PostgreSQL password |
| `PGPORT` | PostgreSQL port (default: 5432) |

## License

Apache License 2.0 - see [LICENSE](./LICENSE) for details.
