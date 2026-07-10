# Database Plan

## Purpose
Store players, registrations, payments, and event outcomes.

## Entities (Draft)
- players
- registrations
- payments
- events
- match_participants
- eliminations
- admin_actions

## Minimum Fields
- players: id, gamertag, email, status, created_at
- registrations: id, player_id, event_id, state, notes, created_at
- payments: id, player_id, provider, amount, currency, status, evidence_url, verified_by, verified_at
- eliminations: id, event_id, player_id, cause, remaining_lives, timestamp

## Requirements
- Unique gamertag per active player
- Unique email per account
- History preserved for all payment state changes
- Full auditability for admin actions

## Migration Strategy
- Start with simple relational schema
- Add indexing after initial test load
- Back up before each event window
