# Hunger Games Backend Starter

This folder now includes a runnable Fastify API scaffold plus PostgreSQL schema.

## Included
- schema.sql: relational schema for players, applications, payments, and match logs.
- openapi.yaml: endpoint contract for registration, payment verification, and status lookup.
- src/server.js: Fastify API implementation.
- src/migrate.js: schema migration runner.
- src/db.js: Postgres pool and transaction helpers.

## Quick Start
1. Copy `.env.example` to `.env` and set `DATABASE_URL`.
2. Set `ADMIN_API_KEYS` in `.env` (comma-separated keys).
3. Install dependencies: `npm install`
4. Run migration: `npm run migrate`
5. Start API: `npm run dev`

## Admin Auth
- Protected admin endpoints require both headers:
	- `x-api-key`: must match one of `ADMIN_API_KEYS`
	- `x-admin-role`: `admin` or `moderator` for read routes, `admin` for write routes

- Admin read routes:
	- `GET /applications`
	- `GET /applications/:id`
	- `GET /admin/events`

- Admin write routes:
	- `PATCH /applications/:id`
	- `PATCH /payments/:id/status`
	- `POST /admin/events`

## Current Endpoints
- POST /applications
- GET /applications
- GET /applications/:id
- PATCH /applications/:id
- POST /payments/evidence
- PATCH /payments/:id/status
- GET /admin/events
- POST /admin/events
- GET /players/status/:gamertag

## Event Bootstrap
- API route (admin): `POST /admin/events`
- Event list route (admin): `GET /admin/events?state=planned&from=2026-07-01T00:00:00Z&to=2026-07-31T23:59:59Z&limit=100`
- CLI shortcut:
	- `npm run seed:event -- --code=hg-qualifier --name="HG Qualifier" --state=planned --startsAt=2026-07-25T18:00:00Z`

## Next Hardening Steps
- Add rate limiting to public routes.
- Add upload endpoint for screenshot files (currently URL metadata only).
- Add admin action audit logging middleware.
