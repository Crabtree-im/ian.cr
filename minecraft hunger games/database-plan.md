# Database Plan

## Purpose
Store players, registrations, payments, and event outcomes.

## Entities (Draft)
- players
- applications
- payments
- events
- match_participants
- eliminations
- admin_actions

## Minimum Fields
- players: id, gamertag, email, status, created_at
- applications: id, player_id, event_id, state, source, notes, created_at
- payments: id, player_id, provider, amount, currency, status, evidence_url, evidence_type, verified_by, verified_at
- eliminations: id, event_id, player_id, cause, remaining_lives, eliminated, timestamp
- admin_actions: id, admin_id, action_type, target_type, target_id, details_json, created_at

## State Enums (Draft)
- application.state: pending, accepted, rejected
- payment.status: pending, accepted
- player.status: applied, approved, eliminated, banned

## Requirements
- Unique gamertag per active player
- Unique email per account
- History preserved for all payment state changes
- Full auditability for admin actions
- Keep evidence URL/file reference for manual Patreon verification
- Preserve elimination event history for post-match audit

## Migration Strategy
- Start with simple relational schema
- Add indexing after initial test load
- Back up before each event window
