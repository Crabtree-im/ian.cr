# Server Plugin Plan

## Purpose
Implement all authoritative game logic on the server side.

## Platform Decision
- Target stack: Paper API plugin.
- Keep gameplay logic server-authoritative (not client-resource-pack-authoritative).

## Scope
- Match state machine (idle, lobby-countdown, spawn-lock-countdown, live, finale, finished)
- 3-lives system
- Death event handling for all causes
- Elimination and lockout/ban flow
- Spawn assignment across 4 sections and configurable spawn pool sizes
- Early movement penalty during countdown
- Chest loot randomization and tier behavior
- Broadcast events (kills, eliminations, player count)

## Data Needed
- Registered player list (approved)
- Arena spawn point definitions
- Chest locations and chest tier tags
- Life totals and elimination state

## Config Requirements
- `player_limit` (default 100)
- `lives_per_player` (default 3)
- `sections` (default 4)
- `lobby_countdown_seconds` (default 10)
- `spawn_lock_countdown_seconds` (default 60)
- `finale_location`
- `allow_rejoin_when_eliminated` (default false)

## Match Flow (Draft)
- Admin runs `/hg startgames`.
- State changes to lobby-countdown for 10 seconds.
- Players are teleported to assigned section start spots.
- State changes to spawn-lock-countdown for 60 seconds.
- Leaving assigned block during spawn-lock-countdown triggers instant death and respawn at assigned start.
- State changes to live after countdown ends.
- On life reaching 0, player is eliminated and kicked.
- Final surviving player enters finale state and is teleported to `finale_location`.

## Spawn Assignment Approach
- Even distribution across 4 sections when possible.
- Within each section, assign players randomly to available named spawn points.
- Spawn points use section identifiers such as red, orange, blue, purple plus index labels.
- If player count is not divisible by 4, extra players are spread one-by-one to sections.

## Commands (Draft)
- /hg setup
- /hg registerspawn <section> <slot>
- /hg registerchest <tier>
- /hg startgames
- /hg stop
- /hg status
- /hg setlives <player> <value>
- /hg setfinale
- /hg assignspawns
- /hg givechest <tier>

## Loot System Direction (Draft)
- Option A: Auto-generate/fill tagged chests in configured regions.
- Option B: Admin command gives prefilled chest item; placed chest keeps generated contents.
- Initial implementation should prefer Option B to simplify testing and balancing.

## Loot Tiers (Draft)
- Normal chest tiers:
	- Tier 1: leather armor focus
	- Tier 2: chainmail armor focus
	- Tier 3: copper armor focus
	- Tier 4: iron armor focus
- Copper chest tiers:
	- Tier 1: utilities + some high-value base items
	- Tier 2: mixed diamond tools/gear + food + wind charges
	- Tier 3: stronger diamond loadouts + pearls + golden apples + cobwebs

## Loot Content Notes (Draft)
- Normal chests can include:
	- Leather/chain/copper/gold/iron gear
	- Food stacks up to 10 of a type (for example bread, carrots, potatoes, berries)
	- Basic tools/weapons (stone, copper, iron variants)
	- Arrows up to 10
- Copper chests can include:
	- Ender pearls, wind charges
	- Diamond gear/tools/weapons
	- Better cooked food
	- XP bottles up to 10
	- Golden apples up to 5
	- Enchanted books, lapis up to 10
	- Rare bow with infinity plus single arrow

## Risks
- Plugin compatibility with host
- Anti-cheat conflicts with custom mechanics
- Performance with 100 concurrent players
- Balance volatility while chest tiers are still evolving
