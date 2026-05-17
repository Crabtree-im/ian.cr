# Gravity Chaos — Project Docs

## Overview
A Godot 4 single-file space survival game with intentionally bad physics.
The player pilots a spaceship and must survive an ever-growing field of meteors
while gravity randomly shifts direction every few seconds.

## Files
| File | Purpose |
|---|---|
| `gravity_chaos.gd` | All game logic and rendering (~320 lines, no nodes) |
| `Main.tscn` | Minimal scene — one Node2D with `gravity_chaos.gd` attached |
| `project.godot` | Godot 4 project config, 1280×720, canvas_items stretch |

## Controls
| Key | Action |
|---|---|
| `W` | Thrust forward |
| `S` | Brake |
| `A` / `D` | Rotate ship |
| `Space` | Restart (on death screen) |

## Game Rules
- **Score** increases +5 points per second while alive
- **Death** occurs on contact with any meteor (while unshielded)
- **Best score** is kept across restarts within the same session

## Physics (intentionally wrong)
- **Gravity is sideways** — starts pointing right, not down
- **Gravity flips** — smoothly lerps to a random new direction every 4–9 seconds
- **Meteors bounce off walls** with restitution > 1 (gain energy each bounce)
- **Meteors bounce off each other** — elastic collision with randomized spin on impact
- **Meteors are affected by gravity** at 55% of the ship's gravity strength
- **Ship wraps** around screen edges (no walls for the player)

## Shield Power-Up
- A glowing cyan orb (marked **S**) spawns on the field at game start
- Fly into it to collect — activates a **5-second bubble** around your ship
- While shielded, a meteor hit **knocks the meteor away** instead of killing you, then the shield is consumed
- After collection, the next shield respawns in **20–30 seconds** at a random position
- A cyan timer bar below the score shows remaining shield time

## Key Tunables (top of `gravity_chaos.gd`)
| Constant | Value | Effect |
|---|---|---|
| `METEOR_COUNT_START` | 6 | Meteors at game start |
| `METEOR_SPAWN_INTERVAL` | 5.0s | New meteor every N seconds |
| `METEOR_RESTITUTION` | 1.06 | Wall/collision bounce energy gain |
| `GRAVITY_STRENGTH` | 180.0 | Pull force on ship and meteors |
| `GRAVITY_FLIP_MIN/MAX` | 4–9s | Random flip interval range |
| `GRAVITY_LERP_SPEED` | 1.4 | How fast gravity transitions |
| `SHIP_THRUST` | 440.0 | Forward acceleration |
| `SHIP_SPEED_CAP` | 580.0 | Max ship velocity |
| `SHIP_HITBOX` | 13.0 | Collision radius (pixels) |
| `SCORE_PER_SECOND` | 5.0 | Points earned per second alive |
| `SHIELD_DURATION` | 5.0s | How long a collected shield lasts |
| `SHIELD_SPAWN_MIN/MAX` | 20–30s | Respawn window after collection |

## HUD Elements
- **Gravity compass** — top-left circle showing current gravity direction and needle
- **Flip countdown bar** — top-right orange bar showing time until next gravity flip
- **Score + time** — top-center while playing
- **Shield timer bar** — cyan bar below score, visible only while shield is active
- **Death screen** — shows final score, best score, and restart prompt

## Rendering
- All drawing done via `_draw()` / `queue_redraw()` — no Sprites or nodes
- Static starfield uses a seeded RNG so stars never move
- Ship has: engine exhaust glow (while thrusting), hull polygon, cockpit highlight, motion trail, pulsing shield bubble
- Meteors have: procedural rock shape (8–13 vertices), glow halo, crater detail, per-meteor rotation that changes on collision
- Shield pickup has: pulsing outer glow, cyan ring outline, S label

## Ideas / Future Spice
- [x] Shield power-up
- [x] Meteor-meteor collisions
- [ ] Meteor splitting on wall collision
- [ ] Difficulty ramp (gravity flips faster over time)
- [ ] Particle explosion on death
- [ ] Speed burst power-up
- [ ] Sound effects via AudioStreamPlayer
- [ ] High score persistence via ConfigFile
