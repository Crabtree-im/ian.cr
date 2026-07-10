package com.btree.hungergames.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PluginConfig {
    private final int playerLimit;
    private final int livesPerPlayer;
    private final int lobbyCountdownSeconds;
    private final int spawnLockCountdownSeconds;
    private final boolean allowRejoinWhenEliminated;
    private final List<String> sections;

    private PluginConfig(
            int playerLimit,
            int livesPerPlayer,
            int lobbyCountdownSeconds,
            int spawnLockCountdownSeconds,
            boolean allowRejoinWhenEliminated,
            List<String> sections
    ) {
        this.playerLimit = playerLimit;
        this.livesPerPlayer = livesPerPlayer;
        this.lobbyCountdownSeconds = lobbyCountdownSeconds;
        this.spawnLockCountdownSeconds = spawnLockCountdownSeconds;
        this.allowRejoinWhenEliminated = allowRejoinWhenEliminated;
        this.sections = sections;
    }

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        return new PluginConfig(
                c.getInt("match.player_limit", 100),
                c.getInt("match.lives_per_player", 3),
                c.getInt("match.lobby_countdown_seconds", 10),
                c.getInt("match.spawn_lock_countdown_seconds", 60),
                c.getBoolean("match.allow_rejoin_when_eliminated", false),
                c.getStringList("sections")
        );
    }

    public int getPlayerLimit() {
        return playerLimit;
    }

    public int getLivesPerPlayer() {
        return livesPerPlayer;
    }

    public int getLobbyCountdownSeconds() {
        return lobbyCountdownSeconds;
    }

    public int getSpawnLockCountdownSeconds() {
        return spawnLockCountdownSeconds;
    }

    public boolean isAllowRejoinWhenEliminated() {
        return allowRejoinWhenEliminated;
    }

    public List<String> getSections() {
        return sections;
    }
}
