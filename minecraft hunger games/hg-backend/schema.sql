-- Hunger Games backend schema (MySQL 8.0+)
-- Run via: python migrate.py

CREATE TABLE IF NOT EXISTS players (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    gamertag   VARCHAR(32)     NOT NULL,
    email      VARCHAR(255)    NOT NULL,
    status     VARCHAR(32)     NOT NULL DEFAULT 'applied',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_gamertag (gamertag),
    UNIQUE KEY uq_email    (email),
    KEY        idx_status  (status)
);

CREATE TABLE IF NOT EXISTS events (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(64)     NOT NULL,
    name       VARCHAR(255)    NOT NULL,
    state      VARCHAR(32)     NOT NULL DEFAULT 'planned',
    starts_at  DATETIME        NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_code (code)
);

CREATE TABLE IF NOT EXISTS applications (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id  BIGINT UNSIGNED NOT NULL,
    event_id   BIGINT UNSIGNED NOT NULL,
    state      VARCHAR(32)     NOT NULL DEFAULT 'pending',
    source     VARCHAR(32)     NOT NULL DEFAULT 'website',
    notes      TEXT            NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_player_event (player_id, event_id),
    KEY        idx_state       (state),
    CONSTRAINT fk_app_player FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE,
    CONSTRAINT fk_app_event  FOREIGN KEY (event_id)  REFERENCES events (id)  ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id      BIGINT UNSIGNED NOT NULL,
    event_id       BIGINT UNSIGNED NOT NULL,
    receipt_code   VARCHAR(128)    NOT NULL,
    screenshot_url TEXT            NOT NULL,
    status         VARCHAR(32)     NOT NULL DEFAULT 'pending',
    verified_by    VARCHAR(64)     NULL,
    verified_at    DATETIME        NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_receipt_code (receipt_code),
    KEY        idx_status      (status),
    CONSTRAINT fk_pay_player FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE,
    CONSTRAINT fk_pay_event  FOREIGN KEY (event_id)  REFERENCES events (id)  ON DELETE CASCADE
);
