import dotenv from "dotenv";
import Fastify from "fastify";
import { z } from "zod";
import { query, withTransaction, pool } from "./db.js";

dotenv.config();

const fastify = Fastify({ logger: true });

const adminApiKeys = new Set(
  String(process.env.ADMIN_API_KEYS || "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean)
);

function hasValidAdminKey(request) {
  const key = String(request.headers["x-api-key"] || "").trim();
  return key.length > 0 && adminApiKeys.has(key);
}

function getRole(request) {
  return String(request.headers["x-admin-role"] || "").trim().toLowerCase();
}

async function requireAdminRead(request, reply) {
  if (!hasValidAdminKey(request)) {
    return reply.code(401).send({ error: "missing or invalid admin api key" });
  }
  const role = getRole(request);
  if (role !== "admin" && role !== "moderator") {
    return reply.code(403).send({ error: "admin or moderator role required" });
  }
  return undefined;
}

async function requireAdminWrite(request, reply) {
  if (!hasValidAdminKey(request)) {
    return reply.code(401).send({ error: "missing or invalid admin api key" });
  }
  const role = getRole(request);
  if (role !== "admin") {
    return reply.code(403).send({ error: "admin role required" });
  }
  return undefined;
}

const createApplicationSchema = z.object({
  gamertag: z.string().min(3).max(32),
  email: z.string().email(),
  eventCode: z.string().min(1).max(64),
  source: z.string().min(1).max(32).default("discord"),
  notes: z.string().max(2000).optional()
});

const updateApplicationSchema = z.object({
  state: z.enum(["pending", "accepted", "rejected"]),
  notes: z.string().max(2000).optional()
});

const paymentEvidenceSchema = z.object({
  playerId: z.number().int().positive(),
  eventId: z.number().int().positive(),
  evidenceUrl: z.string().url(),
  provider: z.string().min(1).max(64).default("patreon")
});

const updatePaymentStatusSchema = z.object({
  status: z.enum(["pending", "accepted"]),
  verifiedBy: z.string().min(1).max(64).optional()
});

const createEventSchema = z.object({
  code: z.string().min(1).max(64),
  name: z.string().min(1).max(255),
  state: z.string().min(1).max(32).default("planned"),
  startsAt: z.string().datetime().optional()
});

const listEventsQuerySchema = z.object({
  state: z.string().min(1).max(32).optional(),
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional(),
  limit: z.coerce.number().int().min(1).max(500).default(100)
});

const listPlayersQuerySchema = z.object({
  status: z.enum(["applied", "approved", "eliminated", "banned"]).optional(),
  limit: z.coerce.number().int().min(1).max(500).default(100),
  offset: z.coerce.number().int().min(0).default(0)
});

const updatePlayerSchema = z.object({
  status: z.enum(["applied", "approved", "eliminated", "banned"])
});

fastify.get("/health", async () => ({ status: "ok" }));

fastify.post("/applications", async (request, reply) => {
  const parsed = createApplicationSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const payload = parsed.data;

  try {
    const result = await withTransaction(async (client) => {
      const playerRow = await client.query(
        `
        insert into players (gamertag, email, status)
        values ($1, $2, 'applied')
        on conflict (gamertag)
        do update set email = excluded.email
        returning id, gamertag, email, status
        `,
        [payload.gamertag, payload.email]
      );

      const eventRow = await client.query(
        `select id, code from events where code = $1 limit 1`,
        [payload.eventCode]
      );
      if (eventRow.rowCount === 0) {
        throw new Error("eventCode not found");
      }

      const player = playerRow.rows[0];
      const event = eventRow.rows[0];

      const appRow = await client.query(
        `
        insert into applications (player_id, event_id, state, source, notes)
        values ($1, $2, 'pending', $3, $4)
        on conflict (player_id, event_id)
        do update set source = excluded.source, notes = excluded.notes, updated_at = now()
        returning id, player_id, event_id, state, source, notes, created_at, updated_at
        `,
        [player.id, event.id, payload.source, payload.notes ?? null]
      );

      return { player, application: appRow.rows[0] };
    });

    return reply.code(201).send(result);
  } catch (error) {
    if (error instanceof Error && error.message === "eventCode not found") {
      return reply.code(400).send({ error: "eventCode not found" });
    }
    throw error;
  }
});

fastify.get("/applications", { preHandler: requireAdminRead }, async () => {
  const rows = await query(
    `
    select a.id, a.state, a.source, a.notes, a.created_at, a.updated_at,
           p.id as player_id, p.gamertag, p.email,
           e.id as event_id, e.code as event_code
    from applications a
    join players p on p.id = a.player_id
    join events e on e.id = a.event_id
    order by a.created_at desc
    limit 500
    `
  );
  return { applications: rows.rows };
});

fastify.get("/applications/:id", { preHandler: requireAdminRead }, async (request, reply) => {
  const id = Number(request.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return reply.code(400).send({ error: "invalid id" });
  }

  const result = await query(
    `
    select a.id, a.state, a.source, a.notes, a.created_at, a.updated_at,
           p.id as player_id, p.gamertag, p.email,
           e.id as event_id, e.code as event_code
    from applications a
    join players p on p.id = a.player_id
    join events e on e.id = a.event_id
    where a.id = $1
    `,
    [id]
  );

  if (result.rowCount === 0) {
    return reply.code(404).send({ error: "application not found" });
  }

  return result.rows[0];
});

fastify.patch("/applications/:id", { preHandler: requireAdminWrite }, async (request, reply) => {
  const id = Number(request.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return reply.code(400).send({ error: "invalid id" });
  }

  const parsed = updateApplicationSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const payload = parsed.data;
  const result = await query(
    `
    update applications
    set state = $2,
        notes = coalesce($3, notes),
        updated_at = now()
    where id = $1
    returning id, player_id, event_id, state, source, notes, created_at, updated_at
    `,
    [id, payload.state, payload.notes ?? null]
  );

  if (result.rowCount === 0) {
    return reply.code(404).send({ error: "application not found" });
  }

  return result.rows[0];
});

fastify.post("/payments/evidence", async (request, reply) => {
  const parsed = paymentEvidenceSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const payload = parsed.data;
  const result = await query(
    `
    insert into payments (player_id, event_id, provider, status, evidence_url, evidence_type)
    values ($1, $2, $3, 'pending', $4, 'screenshot')
    returning id, player_id, event_id, provider, status, evidence_url, evidence_type, created_at
    `,
    [payload.playerId, payload.eventId, payload.provider, payload.evidenceUrl]
  );

  return reply.code(201).send(result.rows[0]);
});

fastify.post("/admin/events", { preHandler: requireAdminWrite }, async (request, reply) => {
  const parsed = createEventSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const payload = parsed.data;
  const result = await query(
    `
    insert into events (code, name, state, starts_at)
    values ($1, $2, $3, $4)
    on conflict (code)
    do update set
      name = excluded.name,
      state = excluded.state,
      starts_at = excluded.starts_at
    returning id, code, name, state, starts_at, created_at
    `,
    [payload.code, payload.name, payload.state, payload.startsAt ?? null]
  );

  return reply.code(201).send(result.rows[0]);
});

fastify.get("/admin/events", { preHandler: requireAdminRead }, async (request, reply) => {
  const parsed = listEventsQuerySchema.safeParse(request.query || {});
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const { state, from, to, limit } = parsed.data;
  const result = await query(
    `
    select id, code, name, state, starts_at, created_at
    from events
    where ($1::text is null or state = $1)
      and ($2::timestamptz is null or starts_at >= $2)
      and ($3::timestamptz is null or starts_at <= $3)
    order by coalesce(starts_at, created_at) desc, id desc
    limit $4
    `,
    [state ?? null, from ?? null, to ?? null, limit]
  );

  return {
    filters: { state: state ?? null, from: from ?? null, to: to ?? null, limit },
    count: result.rowCount,
    events: result.rows
  };
});

fastify.patch("/payments/:id/status", { preHandler: requireAdminWrite }, async (request, reply) => {
  const id = Number(request.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return reply.code(400).send({ error: "invalid id" });
  }

  const parsed = updatePaymentStatusSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const payload = parsed.data;
  const result = await query(
    `
    update payments
    set status = $2,
        verified_by = coalesce($3, verified_by),
        verified_at = case when $2 = 'accepted' then now() else verified_at end
    where id = $1
    returning id, player_id, event_id, provider, status, evidence_url, verified_by, verified_at, created_at
    `,
    [id, payload.status, payload.verifiedBy ?? null]
  );

  if (result.rowCount === 0) {
    return reply.code(404).send({ error: "payment not found" });
  }

  return result.rows[0];
});

fastify.get("/players", { preHandler: requireAdminRead }, async (request, reply) => {
  const parsed = listPlayersQuerySchema.safeParse(request.query || {});
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const { status, limit, offset } = parsed.data;
  const result = await query(
    `
    select id, gamertag, email, status, created_at
    from players
    where ($1::text is null or status = $1)
    order by created_at desc, id desc
    limit $2
    offset $3
    `,
    [status ?? null, limit, offset]
  );

  return {
    filters: { status: status ?? null, limit, offset },
    count: result.rowCount,
    players: result.rows
  };
});

fastify.patch("/players/:id", { preHandler: requireAdminWrite }, async (request, reply) => {
  const id = Number(request.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return reply.code(400).send({ error: "invalid id" });
  }

  const parsed = updatePlayerSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send({ error: parsed.error.flatten() });
  }

  const result = await query(
    `
    update players
    set status = $2
    where id = $1
    returning id, gamertag, email, status, created_at
    `,
    [id, parsed.data.status]
  );

  if (result.rowCount === 0) {
    return reply.code(404).send({ error: "player not found" });
  }

  return result.rows[0];
});

fastify.get("/players/status/:gamertag", async (request, reply) => {
  const gamertag = String(request.params.gamertag || "").trim();
  if (!gamertag) {
    return reply.code(400).send({ error: "gamertag required" });
  }

  const result = await query(
    `
    select p.gamertag,
           p.status as player_status,
           a.state as application_state,
           pay.status as payment_status,
           e.code as event_code
    from players p
    left join applications a on a.player_id = p.id
    left join events e on e.id = a.event_id
    left join lateral (
      select status from payments pay
      where pay.player_id = p.id
      order by pay.created_at desc
      limit 1
    ) pay on true
    where lower(p.gamertag) = lower($1)
    order by a.created_at desc nulls last
    limit 1
    `,
    [gamertag]
  );

  if (result.rowCount === 0) {
    return reply.code(404).send({ error: "player not found" });
  }

  return result.rows[0];
});

const port = Number(process.env.PORT || 3000);
const host = process.env.HOST || "0.0.0.0";

const start = async () => {
  try {
    await fastify.listen({ port, host });
  } catch (error) {
    fastify.log.error(error);
    await pool.end();
    process.exit(1);
  }
};

const shutdown = async () => {
  await fastify.close();
  await pool.end();
  process.exit(0);
};

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);

start();
