# URL Shortener

A URL shortening service with customizable short links, TTL (Time-To-Live) expiration support, and API key authentication. Generates short unique identifiers for long URLs and handles automatic expiration.

## Features

- Short URL generation with unique identifiers
- TTL-based URL expiration
- API key authentication
- Fallback URL redirect for expired/invalid links
- Health check endpoint
- PostgreSQL persistence via Prisma ORM

## Tech Stack

- Node.js with Express
- Prisma ORM
- PostgreSQL
- Express Validator

## Prerequisites

- Node.js 18+
- PostgreSQL 14+

## Setup

1. Configure environment variables:
   ```bash
   export DATABASE_URL="postgresql://user:password@localhost:5432/urlshortner?schema=public"
   export VALID_API_KEYS="your-api-key-1,your-api-key-2"
   ```

2. Run database migrations:
   ```bash
   npx prisma migrate deploy
   ```

3. Install and run:
   ```bash
   npm install
   npm start
   ```

### Dev mode (against the shared dev infra)

Start the backing infra once from the repo root, then run this service on the host:

```bash
docker compose -f docker-compose.dev.yml up -d        # postgres/minio/es/maildev on localhost
cp .env.example .env                                  # then set the values below
```

`.env` for the dev Postgres (db `urlshortner`):

```bash
PORT=3000
DATABASE_URL=postgresql://admin:admin@localhost:5432/urlshortner?schema=public
VALID_API_KEYS=dev-key
FALLBACK_URL=http://localhost:8080
```

`start.sh` runs `prisma generate` + `migrate deploy` then starts the server:

```bash
./start.sh
```

## API

### Create Short URL

```bash
curl -X POST http://localhost:3000/ \
  -H "Content-Type: application/json" \
  -H "x-api-key: your-api-key" \
  -d '{"url": "https://example.com", "ttl": 1735689600}'
```

### Redirect

```
GET http://localhost:3000/:id
```

### Health Check

```
GET http://localhost:3000/liveliness/health
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL connection URL |
| `VALID_API_KEYS` | Comma-separated list of valid API keys |
| `PORT` | Server port (default: 3000) |

## License

Apache License 2.0 - see [LICENSE](./LICENSE) for details.
