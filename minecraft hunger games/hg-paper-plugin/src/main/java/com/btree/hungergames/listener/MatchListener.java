package com.btree.hungergames.listener;

import com.btree.hungergames.match.MatchManager;
import com.btree.hungergames.match.MatchState;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class MatchListener implements Listener {
    private final MatchManager matchManager;

    public MatchListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!matchManager.isLiveOrLocked()) {
            return;
        }
        Player player = event.getPlayer();
        String cause = player.getLastDamageCause() == null ? "unknown" : player.getLastDamageCause().getCause().name();
        matchManager.consumeLife(player, cause);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (matchManager.getState() == MatchState.IDLE || matchManager.getState() == MatchState.FINISHED) {
            return;
        }
        if (matchManager.isEliminated(event.getPlayer().getUniqueId())) {
            event.getPlayer().kickPlayer("You are eliminated from this Hunger Games match.");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (matchManager.getState() != MatchState.SPAWN_LOCK_COUNTDOWN) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "You moved too early.");
            player.setHealth(0.0);
        }
    }
}
