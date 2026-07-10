# Server Plugin Plan

## Purpose
Implement all authoritative game logic on the server side.

## Scope
- Match state machine (lobby, countdown, live, finished)
- 3-lives system
- Death event handling for all causes
- Elimination and lockout/ban flow
- Spawn assignment across 4 sections and 4 cornucopias
- Early movement penalty during countdown
- Chest loot randomization and refill behavior
- Broadcast events (kills, eliminations, player count)

## Data Needed
- Registered player list (approved)
- Arena spawn point definitions
- Chest locations and chest tier tags
- Life totals and elimination state

## Commands (Draft)
- /hg setup
- /hg registerspawn <section> <slot>
- /hg registerchest <tier>
- /hg start
- /hg stop
- /hg status
- /hg setlives <player> <value>

## Risks
- Plugin compatibility with host
- Anti-cheat conflicts with custom mechanics
- Performance with 100 concurrent players
