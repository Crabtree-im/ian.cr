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
2. Install dependencies: `npm install`
3. Run migration: `npm run migrate`
4. Start API: `npm run dev`

## Current Endpoints
- POST /applications
- GET /applications
- GET /applications/:id
- PATCH /applications/:id
- POST /payments/evidence
- PATCH /payments/:id/status
- GET /players/status/:gamertag

## Next Hardening Steps
- Add admin authentication and role checks.
- Add rate limiting to public routes.
- Add upload endpoint for screenshot files (currently URL metadata only).
- Add event creation/admin endpoints.
