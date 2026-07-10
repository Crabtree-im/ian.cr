# Game Rules

## Match Format
- Player cap is configurable, with 100 as the default target.
- Arena is split into 4 biome sections.
- Each section has named starting spots (for example: purple-01, red-12, blue-07).
- Players are distributed as evenly as possible across the 4 sections.

## Lives System
- Every player starts with 3 lives.
- Any death cause removes 1 life: PvP, void, fire/lava, drowning, explosion, fall, mob, and other vanilla causes.
- On deaths where lives remain:
	- Player respawns and can continue the match.
- On death with no lives remaining:
	- Player is eliminated.
	- Player sees a banned-style death screen.
	- Player is kicked from the server.
	- Rejoining during active match is blocked.
- Eliminated players cannot spectate or monitor the game.

## Start Sequence
- Admin runs a start command to initiate pre-match flow.
- A 10-second lobby transition countdown runs first.
- Players are teleported to assigned section spawn blocks.
- A 60-second pre-game countdown runs while players must remain on their assigned block.
- If a player leaves their block during the 60-second countdown:
	- They instantly die (life is consumed).
	- They respawn back on their assigned starting block.
- Once game state switches to live:
	- Spawn-lock behavior is disabled.
	- Bed spawn behavior returns to vanilla expectations.

## Winner Condition
- Last active player wins.
- Winner ceremony:
	- Hunger Games theme plays.
	- Eliminated players and their biome origins are replayed/displayed.
	- Winner is teleported to a configured `finale` location.
	- Admins complete prize verification and payout steps.

## Tie Resolution
- If final 2 players die near-simultaneously, winner is resolved by deterministic server ordering.
- Resolution order:
	- 1) Later server tick death event wins.
	- 2) If same tick, later event sequence index wins.
	- 3) If still tied, higher pre-death health wins.
	- 4) If still tied, randomized tiebreak using match seed + player UUID hash.
- Winner is revived automatically with recorded pre-elimination gear snapshot and teleported to finale flow.

## Fairness Notes
- 3-life format is intentionally designed to increase playtime value for paid participants.
- Spawn assignment and elimination logic should be auditable through logs.
