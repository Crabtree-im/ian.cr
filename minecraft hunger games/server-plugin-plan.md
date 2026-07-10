# Server Plugin Plan

## Purpose
Implement all authoritative game logic on the server side.

## Platform Decision
- Target stack: Paper API plugin.
- Keep gameplay logic server-authoritative (not client-resource-pack-authoritative).

## Plugin Blueprint
- Suggested plugin name: HungerGamesCore
- Suggested package: com.btree.hungergames
- Java target: 21 (match current Paper support for selected server version)
- Persistence:
	- YAML for config and arena setup
	- Optional SQL in phase 2 for long-term event analytics

## Scope
- Match state machine (idle, lobby-countdown, spawn-lock-countdown, live, finale, finished)
- 3-lives system
- Death event handling for all causes
- Elimination and lockout/ban flow
- Spawn assignment across 4 sections and configurable spawn pool sizes
- Early movement penalty during countdown
- Chest loot randomization and tier behavior
- Broadcast events (kills, eliminations, player count)

## Non-Goals (Phase 1)
- No cross-server network sync
- No fully automated Patreon API verification inside plugin
- No spectator mode for eliminated players

## Data Needed
- Registered player list (approved)
- Arena spawn point definitions
- Chest locations and chest tier tags
- Life totals and elimination state

## Internal Data Model (Plugin)
- MatchSession
	- matchId
	- state
	- startedAt
	- alivePlayerIds
	- eliminatedPlayerIds
- PlayerMatchState
	- uuid
	- gamertag
	- sectionId
	- assignedSpawnId
	- livesRemaining
	- eliminated
	- eliminatedAt
- SpawnPoint
	- id (example: purple-01)
	- sectionId
	- world
	- x/y/z/yaw/pitch
- ChestDefinition
	- id
	- tierType (normal, copper)
	- tierLevel
	- world
	- x/y/z

## Config Requirements
- `player_limit` (default 100)
- `lives_per_player` (default 3)
- `sections` (default 4)
- `lobby_countdown_seconds` (default 10)
- `spawn_lock_countdown_seconds` (default 60)
- `finale_location`
- `allow_rejoin_when_eliminated` (default false)

## Config Schema (Draft)
```yaml
match:
	player_limit: 100
	lives_per_player: 3
	sections: [red, orange, blue, purple]
	lobby_countdown_seconds: 10
	spawn_lock_countdown_seconds: 60
	allow_rejoin_when_eliminated: false

locations:
	finale:
		world: world
		x: 0.0
		y: 80.0
		z: 0.0
		yaw: 0.0
		pitch: 0.0

rules:
	prestart_leave_block_penalty: instant_death
	eliminate_on_zero_lives: true
	kick_on_elimination: true

loot:
	fill_mode: admin_prefilled
	normal:
		tier_1_weight: 40
		tier_2_weight: 30
		tier_3_weight: 20
		tier_4_weight: 10
	copper:
		tier_1_weight: 50
		tier_2_weight: 35
		tier_3_weight: 15
```

## Match Flow (Draft)
- Admin runs `/hg startgames`.
- State changes to lobby-countdown for 10 seconds.
- Players are teleported to assigned section start spots.
- State changes to spawn-lock-countdown for 60 seconds.
- Leaving assigned block during spawn-lock-countdown triggers instant death and respawn at assigned start.
- State changes to live after countdown ends.
- On life reaching 0, player is eliminated and kicked.
- Final surviving player enters finale state and is teleported to `finale_location`.

## Event Handling Rules
- On PlayerDeathEvent:
	- Resolve death cause string.
	- Decrement lives by 1.
	- If lives > 0, allow respawn.
	- If lives == 0, mark eliminated, log elimination, and kick.
- On PlayerMoveEvent during spawn-lock-countdown:
	- If moved off assigned start block bounds, trigger instant death.
- On PlayerJoinEvent:
	- If match is live and player is eliminated, deny join with elimination message.
- On PlayerQuitEvent:
	- If live match and not eliminated, treat as forfeit (configurable in phase 2).

## Spawn Assignment Approach
- Even distribution across 4 sections when possible.
- Within each section, assign players randomly to available named spawn points.
- Spawn points use section identifiers such as red, orange, blue, purple plus index labels.
- If player count is not divisible by 4, extra players are spread one-by-one to sections.

## Spawn Assignment Algorithm (Deterministic)
- Input:
	- approvedPlayers list
	- sectionIds list
	- spawnPointsBySection map
- Process:
	- Shuffle players using seeded RNG with matchId as seed.
	- Compute target counts per section using floor division + remainder distribution.
	- Assign players in shuffled order to each section target.
	- Shuffle each section spawn list with same seed namespace.
	- Map players to spawn points by index.
- Output:
	- persisted assignments for the match.

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

## Command Contract (Phase 1)
- /hg startgames
	- permission: hg.admin.start
	- behavior: validates setup, locks roster, starts lobby countdown
- /hg stop
	- permission: hg.admin.stop
	- behavior: force stops match and clears runtime state
- /hg status
	- permission: hg.admin.status
	- behavior: prints state, alive count, eliminated count, timer state
- /hg registerspawn <section> <id>
	- permission: hg.admin.setup
	- behavior: stores player current location as spawn point
- /hg setfinale
	- permission: hg.admin.setup
	- behavior: stores player current location as finale location
- /hg assignspawns
	- permission: hg.admin.setup
	- behavior: precomputes and previews assignments
- /hg setlives <player> <value>
	- permission: hg.admin.moderate
	- behavior: adjusts lives in active match
- /hg givechest <normal|copper> <tier>
	- permission: hg.admin.loot
	- behavior: gives prefilled chest placement item

## Permission Nodes
- hg.admin.start
- hg.admin.stop
- hg.admin.status
- hg.admin.setup
- hg.admin.moderate
- hg.admin.loot

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

## Loot Generation Rules (Phase 1)
- Use weighted item pools by chest tier.
- Enforce slot budget per chest (example: 4 to 7 occupied slots).
- Enforce max stack limits for capped resources:
  - arrows <= 10
  - xp bottles <= 10
  - lapis <= 10
  - golden apples <= 5
- Enforce category diversity target:
  - try for at least one defense item, one offense/tool item, one sustenance/utility item.

## Risks
- Plugin compatibility with host
- Anti-cheat conflicts with custom mechanics
- Performance with 100 concurrent players
- Balance volatility while chest tiers are still evolving

## Logging and Audit
- Log all life changes with cause and remaining lives.
- Log elimination events with timestamp and section.
- Log start/stop commands and acting admin.
- Log spawn assignment output for replayability and dispute resolution.

## Milestones
- M1: Plugin scaffold + config + state machine skeleton
- M2: Spawn registration and assignment + start flow
- M3: Lives, elimination, and rejoin blocking
- M4: Admin chest generation and tiered loot
- M5: Winner finale flow and ceremony hooks
