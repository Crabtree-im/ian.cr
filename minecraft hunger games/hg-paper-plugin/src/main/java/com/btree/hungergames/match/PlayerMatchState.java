package com.btree.hungergames.match;

import java.util.UUID;

public final class PlayerMatchState {
    private final UUID uuid;
    private final String playerName;
    private int livesRemaining;
    private boolean eliminated;
    private String sectionId;
    private String spawnId;

    public PlayerMatchState(UUID uuid, String playerName, int livesRemaining) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.livesRemaining = livesRemaining;
        this.eliminated = false;
        this.sectionId = null;
        this.spawnId = null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getLivesRemaining() {
        return livesRemaining;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public int consumeLife() {
        if (livesRemaining > 0) {
            livesRemaining -= 1;
        }
        if (livesRemaining <= 0) {
            eliminated = true;
        }
        return livesRemaining;
    }

    public void setLivesRemaining(int livesRemaining) {
        this.livesRemaining = Math.max(0, livesRemaining);
        this.eliminated = this.livesRemaining == 0;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getSpawnId() {
        return spawnId;
    }

    public void setSpawnId(String spawnId) {
        this.spawnId = spawnId;
    }
}
