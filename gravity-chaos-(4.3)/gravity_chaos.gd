extends Node2D
## Gravity Chaos — Bad Physics Space Survival
## Survive meteors while chaotic gravity constantly shifts direction.
## W/S = thrust forward/brake  |  A/D = rotate  |  Score +5/sec

# ── tunables ──────────────────────────────────────────────────────────────────
const METEOR_COUNT_START    := 6
const METEOR_SPAWN_INTERVAL := 5.0
const METEOR_MIN_RAD        := 16.0
const METEOR_MAX_RAD        := 40.0
const METEOR_SPEED_MIN      := 100.0
const METEOR_SPEED_MAX      := 280.0
const METEOR_RESTITUTION    := 1.06   # wall bounce gains energy
const METEOR_SPEED_CAP      := 700.0
const GRAVITY_STRENGTH      := 180.0
const GRAVITY_FLIP_MIN      := 4.0
const GRAVITY_FLIP_MAX      := 9.0
const GRAVITY_LERP_SPEED    := 1.4
const SHIP_THRUST           := 440.0
const SHIP_BRAKE            := 200.0
const SHIP_ROTATE_SPEED     := TAU * 0.65
const SHIP_DRAG             := 0.988
const SHIP_SPEED_CAP        := 580.0
const SHIP_HITBOX           := 13.0
const SCORE_PER_SECOND      := 5.0
const TRAIL_LEN             := 22
const SHIELD_DURATION       := 5.0
const SHIELD_RADIUS         := 22.0
const SHIELD_SPAWN_MIN      := 20.0
const SHIELD_SPAWN_MAX      := 30.0

# ── state ─────────────────────────────────────────────────────────────────────
enum Phase { PLAYING, DEAD }
var phase := Phase.PLAYING

var screen: Vector2
var font:   Font

# ship
var ship_pos:   Vector2
var ship_vel:   Vector2
var ship_angle: float
var ship_trail: Array = []

# meteors
var met_pos:    Array[Vector2]             = []
var met_vel:    Array[Vector2]             = []
var met_rad:    Array[float]               = []
var met_angle:  Array[float]               = []
var met_rot:    Array[float]               = []
var met_shape:  Array[PackedVector2Array]  = []

# gravity — smoothly interpolated between flips
var grav_dir:  Vector2
var grav_from: Vector2
var grav_to:   Vector2
var grav_t:    float = 1.0   # 0..1 lerp progress

var flip_timer:  float = 0.0
var flip_target: float = 0.0

# shield pickup
var shield_active:   bool    = false
var shield_timer:    float   = 0.0
var shield_pickup:   bool    = false   # is a pickup currently on the field?
var shield_pos:      Vector2 = Vector2.ZERO
var shield_spawn_t:  float   = 0.0    # countdown to next spawn
var shield_pulse:    float   = 0.0    # animation phase

# game
var score:       float = 0.0
var best_score:  int   = 0
var alive_time:  float = 0.0
var spawn_timer: float = 0.0

var rng := RandomNumberGenerator.new()

# ── ready ─────────────────────────────────────────────────────────────────────
func _ready() -> void:
	rng.randomize()
	screen = get_viewport_rect().size
	font   = ThemeDB.fallback_font
	_start_game()

func _start_game() -> void:
	phase       = Phase.PLAYING
	score       = 0.0
	alive_time  = 0.0
	spawn_timer = 0.0

	ship_pos   = screen * 0.5
	ship_vel   = Vector2.ZERO
	ship_angle = -PI * 0.5   # nose pointing up
	ship_trail.clear()

	met_pos.clear(); met_vel.clear(); met_rad.clear()
	met_angle.clear(); met_rot.clear(); met_shape.clear()

	# sideways gravity to start — bad physics
	grav_dir  = Vector2.RIGHT
	grav_from = grav_dir
	grav_to   = grav_dir
	grav_t    = 1.0
	flip_target = rng.randf_range(GRAVITY_FLIP_MIN, GRAVITY_FLIP_MAX)
	flip_timer  = 0.0

	shield_active  = false
	shield_timer   = 0.0
	shield_pickup  = true
	shield_pos     = Vector2(
			rng.randf_range(60.0, screen.x - 60.0),
			rng.randf_range(60.0, screen.y - 60.0))
	shield_spawn_t = rng.randf_range(SHIELD_SPAWN_MIN, SHIELD_SPAWN_MAX)
	shield_pulse   = 0.0

	for _i in METEOR_COUNT_START:
		_spawn_meteor()

# ── meteor factory ────────────────────────────────────────────────────────────
func _spawn_meteor() -> void:
	var r    := rng.randf_range(METEOR_MIN_RAD, METEOR_MAX_RAD)
	var edge := rng.randi() % 4
	var p: Vector2
	match edge:
		0: p = Vector2(rng.randf_range(0.0, screen.x), -r)
		1: p = Vector2(screen.x + r, rng.randf_range(0.0, screen.y))
		2: p = Vector2(rng.randf_range(0.0, screen.x), screen.y + r)
		_: p = Vector2(-r, rng.randf_range(0.0, screen.y))

	var aim := (screen * 0.5 - p).normalized().rotated(rng.randf_range(-0.9, 0.9))
	var spd := rng.randf_range(METEOR_SPEED_MIN, METEOR_SPEED_MAX)

	met_pos.append(p)
	met_vel.append(aim * spd)
	met_rad.append(r)
	met_angle.append(rng.randf() * TAU)
	met_rot.append(rng.randf_range(-2.0, 2.0))
	met_shape.append(_build_rock(r))

func _build_rock(r: float) -> PackedVector2Array:
	var pts  := PackedVector2Array()
	var segs := rng.randi_range(8, 13)
	for i in segs:
		var a    := float(i) / float(segs) * TAU
		var dist := r * rng.randf_range(0.6, 1.0)
		pts.append(Vector2(cos(a), sin(a)) * dist)
	return pts

# ── input ─────────────────────────────────────────────────────────────────────
func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and not event.is_echo() and event.is_pressed():
		if phase == Phase.DEAD and event.keycode == KEY_SPACE:
			_start_game()

# ── process ───────────────────────────────────────────────────────────────────
func _process(delta: float) -> void:
	if phase == Phase.DEAD:
		queue_redraw()
		return

	alive_time  += delta
	score       += SCORE_PER_SECOND * delta
	spawn_timer += delta
	if spawn_timer >= METEOR_SPAWN_INTERVAL:
		spawn_timer = 0.0
		_spawn_meteor()

	_tick_gravity(delta)
	_tick_shield(delta)
	_move_ship(delta)
	_move_meteors(delta)
	_check_collisions()
	queue_redraw()

# ── shield ───────────────────────────────────────────────────────────────────
func _tick_shield(delta: float) -> void:
	shield_pulse += delta * 3.0

	# count down active shield
	if shield_active:
		shield_timer -= delta
		if shield_timer <= 0.0:
			shield_active = false

	# count down to next pickup spawn
	if not shield_pickup:
		shield_spawn_t -= delta
		if shield_spawn_t <= 0.0:
			shield_pickup = true
			shield_pos    = Vector2(
					rng.randf_range(60.0, screen.x - 60.0),
					rng.randf_range(60.0, screen.y - 60.0))

# ── gravity ───────────────────────────────────────────────────────────────────
func _tick_gravity(delta: float) -> void:
	if grav_t < 1.0:
		grav_t   = minf(grav_t + delta * GRAVITY_LERP_SPEED, 1.0)
		grav_dir = grav_from.lerp(grav_to, grav_t).normalized()

	flip_timer += delta
	if flip_timer >= flip_target:
		flip_timer  = 0.0
		flip_target = rng.randf_range(GRAVITY_FLIP_MIN, GRAVITY_FLIP_MAX)
		var options : Array[float] = [0.0, PI*0.5, PI, PI*1.5, PI*0.25, PI*0.75, PI*1.25, PI*1.75]
		var angle   : float = options[rng.randi() % options.size()]
		grav_from   = grav_dir
		grav_to     = Vector2(cos(angle), sin(angle))
		grav_t      = 0.0

# ── ship movement ─────────────────────────────────────────────────────────────
func _move_ship(delta: float) -> void:
	var rot_input := 0.0
	if Input.is_physical_key_pressed(KEY_A) or Input.is_action_pressed("ui_left"):
		rot_input -= 1.0
	if Input.is_physical_key_pressed(KEY_D) or Input.is_action_pressed("ui_right"):
		rot_input += 1.0
	ship_angle += rot_input * SHIP_ROTATE_SPEED * delta

	var fwd := Vector2(cos(ship_angle), sin(ship_angle))

	if Input.is_physical_key_pressed(KEY_W) or Input.is_action_pressed("ui_up"):
		ship_vel += fwd * SHIP_THRUST * delta
	if Input.is_physical_key_pressed(KEY_S) or Input.is_action_pressed("ui_down"):
		ship_vel -= fwd * SHIP_BRAKE * delta

	ship_vel += grav_dir * GRAVITY_STRENGTH * delta
	ship_vel *= pow(SHIP_DRAG, delta * 60.0)

	var spd := ship_vel.length()
	if spd > SHIP_SPEED_CAP:
		ship_vel = ship_vel * (SHIP_SPEED_CAP / spd)

	var prev_pos := ship_pos
	ship_pos    += ship_vel * delta
	ship_pos.x   = wrapf(ship_pos.x, 0.0, screen.x)
	ship_pos.y   = wrapf(ship_pos.y, 0.0, screen.y)

	if prev_pos.distance_to(ship_pos) < 120.0:
		ship_trail.append(ship_pos)
	else:
		ship_trail.clear()
	if ship_trail.size() > TRAIL_LEN:
		ship_trail.pop_front()

# ── meteor movement ───────────────────────────────────────────────────────────
func _move_meteors(delta: float) -> void:
	var n := met_pos.size()
	for i in n:
		met_vel[i]   += grav_dir * GRAVITY_STRENGTH * delta * 0.55
		met_pos[i]   += met_vel[i] * delta
		met_angle[i] += met_rot[i] * delta

		var spd := met_vel[i].length()
		if spd > METEOR_SPEED_CAP:
			met_vel[i] = met_vel[i] * (METEOR_SPEED_CAP / spd)

		var r := met_rad[i]
		if met_pos[i].x - r < 0.0:
			met_pos[i].x = r
			met_vel[i].x = abs(met_vel[i].x) * METEOR_RESTITUTION
		elif met_pos[i].x + r > screen.x:
			met_pos[i].x = screen.x - r
			met_vel[i].x = -abs(met_vel[i].x) * METEOR_RESTITUTION
		if met_pos[i].y - r < 0.0:
			met_pos[i].y = r
			met_vel[i].y = abs(met_vel[i].y) * METEOR_RESTITUTION
		elif met_pos[i].y + r > screen.y:
			met_pos[i].y = screen.y - r
			met_vel[i].y = -abs(met_vel[i].y) * METEOR_RESTITUTION

	# meteor-meteor elastic collisions
	for i in n:
		for j in range(i + 1, n):
			var diff  := met_pos[j] - met_pos[i]
			var dist  := diff.length()
			var min_d := met_rad[i] + met_rad[j]
			if dist >= min_d or dist < 0.01:
				continue
			# separate
			var normal  := diff.normalized()
			var overlap := (min_d - dist) * 0.5
			met_pos[i] -= normal * overlap
			met_pos[j] += normal * overlap
			# impulse (equal mass elastic)
			var rel   := met_vel[j] - met_vel[i]
			var along := rel.dot(normal)
			if along >= 0.0:
				continue
			var impulse := along * METEOR_RESTITUTION
			met_vel[i] += normal * impulse
			met_vel[j] -= normal * impulse
			# spin change on impact
			met_rot[i] = rng.randf_range(-3.5, 3.5)
			met_rot[j] = rng.randf_range(-3.5, 3.5)

# ── collision ─────────────────────────────────────────────────────────────────
func _check_collisions() -> void:
	# shield pickup collection
	if shield_pickup and ship_pos.distance_to(shield_pos) < SHIP_HITBOX + SHIELD_RADIUS:
		shield_pickup  = false
		shield_active  = true
		shield_timer   = SHIELD_DURATION
		shield_spawn_t = rng.randf_range(SHIELD_SPAWN_MIN, SHIELD_SPAWN_MAX)

	# meteor hits
	for i in met_pos.size():
		if ship_pos.distance_to(met_pos[i]) < SHIP_HITBOX + met_rad[i] * 0.78:
			if shield_active:
				# shield absorbs hit — knock the meteor away instead
				met_vel[i] = (met_pos[i] - ship_pos).normalized() * METEOR_SPEED_MAX * 1.4
				shield_active = false
				shield_timer  = 0.0
				return
			_die()
			return

func _die() -> void:
	phase      = Phase.DEAD
	best_score = maxi(best_score, int(score))

# ── draw ──────────────────────────────────────────────────────────────────────
func _draw() -> void:
	_draw_stars()

	if shield_pickup:
		_draw_shield_pickup()

	for i in met_pos.size():
		_draw_meteor(i)

	if phase == Phase.PLAYING:
		_draw_ship_trail()
		_draw_ship()
		_draw_hud()
	else:
		_draw_death_screen()

	_draw_gravity_compass()
	_draw_flip_bar()

func _draw_stars() -> void:
	var star_rng := RandomNumberGenerator.new()
	star_rng.seed = 7331
	for _i in 140:
		var sx := star_rng.randf() * screen.x
		var sy := star_rng.randf() * screen.y
		var br := star_rng.randf_range(0.25, 0.9)
		var sr := star_rng.randf_range(0.7, 2.2)
		draw_circle(Vector2(sx, sy), sr, Color(br, br, br + 0.1, br * 0.55))

func _draw_shield_pickup() -> void:
	var pulse := (sin(shield_pulse) * 0.5 + 0.5)  # 0..1
	var outer := SHIELD_RADIUS + pulse * 6.0
	draw_circle(shield_pos, outer,       Color(0.2, 0.8, 1.0, 0.18 + pulse * 0.12))
	draw_circle(shield_pos, SHIELD_RADIUS, Color(0.2, 0.8, 1.0, 0.55))
	draw_arc(shield_pos, SHIELD_RADIUS, 0.0, TAU, 32, Color(0.5, 1.0, 1.0, 0.9), 2.0)
	# S icon in centre
	draw_string(font, shield_pos + Vector2(-6.0, 6.0), "S",
		HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color.WHITE)

func _draw_ship_trail() -> void:
	for k in range(1, ship_trail.size()):
		var a := float(k) / float(ship_trail.size()) * 0.55
		draw_line(ship_trail[k - 1], ship_trail[k], Color(0.35, 0.75, 1.0, a), 1.5)

func _draw_ship() -> void:
	var fwd   := Vector2(cos(ship_angle), sin(ship_angle))
	var right := fwd.rotated(PI * 0.5)
	var tip   := ship_pos + fwd  * 20.0
	var bl    := ship_pos - fwd  * 13.0 + right * 11.0
	var br    := ship_pos - fwd  * 13.0 - right * 11.0
	var notch := ship_pos - fwd  *  7.0

	var thrusting := Input.is_physical_key_pressed(KEY_W) or Input.is_action_pressed("ui_up")
	if thrusting:
		draw_circle(ship_pos - fwd * 11.0, 9.0,  Color(1.0, 0.45, 0.05, 0.45))
		draw_circle(ship_pos - fwd * 11.0, 4.5,  Color(1.0, 0.85, 0.4,  0.85))

	var hull := PackedVector2Array([tip, bl, notch, br])
	draw_colored_polygon(hull, Color(0.35, 0.80, 1.0, 0.92))
	draw_polyline(PackedVector2Array([tip, bl, notch, br, tip]), Color.WHITE, 1.6)
	draw_circle(ship_pos + fwd * 5.0, 4.5, Color(0.85, 1.0, 1.0, 0.75))

	# shield bubble
	if shield_active:
		var sp := sin(shield_pulse) * 0.5 + 0.5
		draw_circle(ship_pos, 32.0 + sp * 4.0, Color(0.2, 0.8, 1.0, 0.12 + sp * 0.08))
		draw_arc(ship_pos, 32.0, 0.0, TAU, 48, Color(0.4, 1.0, 1.0, 0.75 + sp * 0.25), 2.5)

func _draw_meteor(i: int) -> void:
	var raw := met_shape[i]
	var pts := PackedVector2Array()
	for p in raw:
		pts.append(met_pos[i] + p.rotated(met_angle[i]))

	draw_circle(met_pos[i], met_rad[i] + 5.0, Color(0.9, 0.5, 0.2, 0.10))
	draw_colored_polygon(pts, Color(0.50, 0.34, 0.20))
	var closed := pts.duplicate()
	closed.append(pts[0])
	draw_polyline(closed, Color(0.80, 0.60, 0.38, 0.85), 1.4)
	draw_circle(met_pos[i] + Vector2(-met_rad[i] * 0.28, -met_rad[i] * 0.22),
		met_rad[i] * 0.20, Color(0.35, 0.24, 0.14, 0.55))

func _draw_gravity_compass() -> void:
	var cx := Vector2(50, 50)
	draw_circle(cx, 30, Color(0.07, 0.07, 0.18, 0.88))
	draw_arc(cx, 30, 0.0, TAU, 48, Color(0.35, 0.45, 0.65, 0.55), 1.4)
	draw_line(cx, cx + grav_dir * 22.0, Color(1.0, 0.55, 0.15, 0.95), 2.0)
	draw_circle(cx + grav_dir * 22.0, 4.0, Color(1.0, 0.55, 0.15))
	draw_string(font, Vector2(16, 90), "GRAVITY",
		HORIZONTAL_ALIGNMENT_LEFT, -1, 10, Color(1.0, 0.6, 0.2, 0.65))

func _draw_flip_bar() -> void:
	var bw   := 148.0
	var bx   := screen.x - bw - 12.0
	var frac := clampf(flip_timer / flip_target, 0.0, 1.0)
	draw_rect(Rect2(bx, 14.0, bw, 7.0), Color(0.12, 0.12, 0.20, 0.80))
	draw_rect(Rect2(bx, 14.0, bw * frac, 7.0), Color(1.0, 0.50, 0.10, 0.90))
	var secs := maxf(0.0, flip_target - flip_timer)
	draw_string(font, Vector2(bx, 34.0), "GRAV FLIP  %.1fs" % secs,
		HORIZONTAL_ALIGNMENT_LEFT, -1, 11, Color(1.0, 0.62, 0.22, 0.80))

func _draw_hud() -> void:
	draw_string(font, Vector2(screen.x * 0.5 - 55.0, 34.0),
		"SCORE  %d" % int(score),
		HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color.WHITE)
	draw_string(font, Vector2(screen.x * 0.5 - 55.0, 54.0),
		"TIME  %.1fs" % alive_time,
		HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color(0.65, 0.80, 1.0, 0.70))
	# shield timer bar
	if shield_active:
		var frac := clampf(shield_timer / SHIELD_DURATION, 0.0, 1.0)
		var bw   := 120.0
		var bx   := screen.x * 0.5 - bw * 0.5
		var by   := 64.0
		draw_rect(Rect2(bx, by, bw, 6.0), Color(0.1, 0.3, 0.4, 0.8))
		draw_rect(Rect2(bx, by, bw * frac, 6.0), Color(0.2, 0.9, 1.0, 0.95))
		draw_string(font, Vector2(bx, by + 20.0), "SHIELD  %.1fs" % shield_timer,
			HORIZONTAL_ALIGNMENT_LEFT, -1, 11, Color(0.4, 1.0, 1.0, 0.9))
	draw_string(font, Vector2(10.0, screen.y - 10.0),
		"W thrust  |  S brake  |  A/D rotate",
		HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color(1, 1, 1, 0.32))

func _draw_death_screen() -> void:
	draw_rect(Rect2(Vector2.ZERO, screen), Color(0, 0, 0, 0.62))
	var cx := screen.x * 0.5
	var cy := screen.y * 0.5
	draw_string(font, Vector2(cx - 95.0, cy - 55.0), "YOU DIED",
		HORIZONTAL_ALIGNMENT_LEFT, -1, 54, Color(1.0, 0.22, 0.18))
	draw_string(font, Vector2(cx - 80.0, cy + 14.0), "SCORE  %d" % int(score),
		HORIZONTAL_ALIGNMENT_LEFT, -1, 28, Color.WHITE)
	draw_string(font, Vector2(cx - 80.0, cy + 50.0), "BEST   %d" % best_score,
		HORIZONTAL_ALIGNMENT_LEFT, -1, 20, Color(0.65, 0.88, 1.0, 0.88))
	draw_string(font, Vector2(cx - 95.0, cy + 94.0), "PRESS  SPACE  TO  PLAY  AGAIN",
		HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color(1, 1, 1, 0.50))
