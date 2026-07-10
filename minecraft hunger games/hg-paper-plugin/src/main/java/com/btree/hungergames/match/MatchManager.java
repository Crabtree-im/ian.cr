package com.btree.hungergames.match;

import com.btree.hungergames.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MatchManager {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final Map<UUID, PlayerMatchState> players = new HashMap<>();

    private MatchState state = MatchState.IDLE;

    public MatchManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public MatchState getState() {
        return state;
    }

    public boolean isLiveOrLocked() {
        return state == MatchState.SPAWN_LOCK_COUNTDOWN || state == MatchState.LIVE;
    }

    public void startGames() {
        if (state != MatchState.IDLE && state != MatchState.FINISHED) {
            return;
        }

        players.clear();
        int registered = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (registered >= config.getPlayerLimit()) {
                break;
            }
            players.put(player.getUniqueId(), new PlayerMatchState(
                    player.getUniqueId(),
                    player.getName(),
                    config.getLivesPerPlayer()
            ));
            registered++;
        }

        state = MatchState.LOBBY_COUNTDOWN;
        Bukkit.broadcastMessage(ChatColor.GOLD + "[HG] Games starting in " + config.getLobbyCountdownSeconds() + "s...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            state = MatchState.SPAWN_LOCK_COUNTDOWN;
            Bukkit.broadcastMessage(ChatColor.YELLOW + "[HG] Spawn lock active for " + config.getSpawnLockCountdownSeconds() + "s.");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                state = MatchState.LIVE;
                Bukkit.broadcastMessage(ChatColor.GREEN + "[HG] The Hunger Games have begun.");
            }, config.getSpawnLockCountdownSeconds() * 20L);
        }, config.getLobbyCountdownSeconds() * 20L);
    }

    public void stopGames() {
        state = MatchState.FINISHED;
        Bukkit.broadcastMessage(ChatColor.RED + "[HG] Match was stopped by admin.");
    }

    public void consumeLife(Player player, String cause) {
        PlayerMatchState p = players.get(player.getUniqueId());
        if (p == null || p.isEliminated()) {
            return;
        }

        int remaining = p.consumeLife();
        Bukkit.broadcastMessage(ChatColor.GRAY + "[HG] " + player.getName() + " lost a life (" + cause + "). "
                + ChatColor.AQUA + remaining + " lives remaining.");

        if (remaining <= 0) {
            eliminate(player, cause);
        }
    }

    public void setLives(Player player, int lives) {
        PlayerMatchState p = players.get(player.getUniqueId());
        if (p == null) {
            return;
        }
        p.setLivesRemaining(lives);
    }

    public boolean isEliminated(UUID uuid) {
        PlayerMatchState p = players.get(uuid);
        return p != null && p.isEliminated();
    }

    public int aliveCount() {
        int alive = 0;
        for (PlayerMatchState p : players.values()) {
            if (!p.isEliminated()) {
                alive++;
            }
        }
        return alive;
    }

    public int eliminatedCount() {
        return players.size() - aliveCount();
    }

    public int getLives(Player player) {
        PlayerMatchState p = players.get(player.getUniqueId());
        return p == null ? 0 : p.getLivesRemaining();
    }

    private void eliminate(Player player, String cause) {
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[HG] " + player.getName() + " was eliminated (" + cause + ").");

        if (!config.isAllowRejoinWhenEliminated()) {
            Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer("You are eliminated from this Hunger Games match."));
        }

        if (aliveCount() == 1 && state == MatchState.LIVE) {
            state = MatchState.FINALE;
            Player winner = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !isEliminated(p.getUniqueId()))
                    .findFirst()
                    .orElse(null);
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "[HG] Winner: " + winner.getName());
            }
            state = MatchState.FINISHED;
        }
    }
}
