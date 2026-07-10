-- Hunger Games backend schema (PostgreSQL-flavored SQL)

create table if not exists players (
    id bigserial primary key,
    gamertag varchar(32) not null unique,
    email varchar(255) not null unique,
    status varchar(32) not null default 'applied',
    created_at timestamptz not null default now()
);

create table if not exists events (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(255) not null,
    state varchar(32) not null default 'planned',
    starts_at timestamptz,
    created_at timestamptz not null default now()
);

create table if not exists applications (
    id bigserial primary key,
    player_id bigint not null references players(id) on delete cascade,
    event_id bigint not null references events(id) on delete cascade,
    state varchar(32) not null default 'pending',
    source varchar(32) not null default 'discord',
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(player_id, event_id)
);

create table if not exists payments (
    id bigserial primary key,
    player_id bigint not null references players(id) on delete cascade,
    event_id bigint not null references events(id) on delete cascade,
    provider varchar(64) not null default 'patreon',
    amount numeric(12, 2),
    currency varchar(8),
    status varchar(32) not null default 'pending',
    evidence_url text,
    evidence_type varchar(32) not null default 'screenshot',
    verified_by varchar(64),
    verified_at timestamptz,
    created_at timestamptz not null default now()
);

create table if not exists match_participants (
    id bigserial primary key,
    event_id bigint not null references events(id) on delete cascade,
    player_id bigint not null references players(id) on delete cascade,
    section_id varchar(32),
    spawn_id varchar(64),
    lives_start int not null default 3,
    lives_remaining int not null default 3,
    eliminated boolean not null default false,
    created_at timestamptz not null default now(),
    unique(event_id, player_id)
);

create table if not exists eliminations (
    id bigserial primary key,
    event_id bigint not null references events(id) on delete cascade,
    player_id bigint not null references players(id) on delete cascade,
    cause varchar(64) not null,
    remaining_lives int not null,
    eliminated boolean not null,
    section_id varchar(32),
    created_at timestamptz not null default now()
);

create table if not exists admin_actions (
    id bigserial primary key,
    admin_id varchar(64) not null,
    action_type varchar(64) not null,
    target_type varchar(64) not null,
    target_id varchar(64) not null,
    details_json jsonb,
    created_at timestamptz not null default now()
);

create index if not exists idx_applications_state on applications(state);
create index if not exists idx_payments_status on payments(status);
create index if not exists idx_participants_event on match_participants(event_id);
create index if not exists idx_eliminations_event on eliminations(event_id);
