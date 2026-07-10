# Backend API Plan

## Purpose
Manage player registration, approval, and event roster administration.

## Core Endpoints (Draft)
- POST /applications
- GET /applications
- GET /applications/{id}
- PATCH /applications/{id}
- POST /payments/evidence
- PATCH /payments/{id}/status
- GET /players
- GET /players/status/{gamertag}
- PATCH /players/{id}

## Registration Source
- Primary intake currently starts from Discord applications.
- Web form can mirror Discord intake and push into the same API.

## Workflow
- Player submits gamertag + email + optional payment screenshot reference.
- Application enters `pending` state.
- Admin reviews Patreon screenshot/payment evidence.
- Payment status is set to `accepted` or `pending`.
- On acceptance, player is marked eligible for whitelist export.

## Player-Facing UI Direction
- Basic HTML site for registration and status checks.
- Player can search gamertag and view application/payment status.
- Public view must hide sensitive payment details.

## Security
- Admin auth for moderation endpoints
- Validation and rate limiting on public submissions
- Audit trail for approval/rejection actions
- Anti-abuse controls on public status lookup endpoint

## Integrations
- Patreon payment reference workflow
- Export/sync for server whitelist
- Discord bot automation for status updates (future phase)
