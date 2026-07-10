# Backend API Plan

## Purpose
Manage player registration, approval, and event roster administration.

## Core Endpoints (Draft)
- POST /registrations
- GET /registrations
- PATCH /registrations/{id}
- POST /payments/verify
- GET /players
- PATCH /players/{id}

## Workflow
- Player submits gamertag + email + payment evidence
- Record enters pending review state
- Admin verifies payment (manual first)
- On approval, player is flagged eligible for whitelist import

## Security
- Admin auth for moderation endpoints
- Validation and rate limiting on public submissions
- Audit trail for approval/rejection actions

## Integrations
- Patreon payment reference workflow
- Export/sync for server whitelist
