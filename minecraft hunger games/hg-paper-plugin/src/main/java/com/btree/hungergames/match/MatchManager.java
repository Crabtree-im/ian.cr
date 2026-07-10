package com.btree.hungergames.match;

import com.btree.hungergames.config.ArenaStore;
import com.btree.hungergames.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class MatchManager {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final ArenaStore arenaStore;
    private final Map<UUID, PlayerMatchState> players = new HashMap<>();
    private final Map<String, List<SpawnPoint>> spawnPointsBySection = new HashMap<>();
    private final Map<UUID, SpawnPoint> assignedSpawns = new HashMap<>();
    private final Map<UUID, Location> lockedStartLocations = new HashMap<>();

    private Location finaleLocation;
    private long currentMatchSeed;

    private MatchState state = MatchState.IDLE;

    public MatchManager(JavaPlugin plugin, PluginConfig config, ArenaStore arenaStore) {
        this.plugin = plugin;
        this.config = config;
        this.arenaStore = arenaStore;
        this.spawnPointsBySection.putAll(arenaStore.loadSpawns());
        this.finaleLocation = arenaStore.loadFinale();
        this.currentMatchSeed = System.currentTimeMillis();
    }

    public MatchState getState() {
        return state;
    }

    public boolean isLiveOrLocked() {
        return state == MatchState.SPAWN_LOCK_COUNTDOWN || state == MatchState.LIVE;
    }

    public String startGames() {
        if (state != MatchState.IDLE && state != MatchState.FINISHED) {
            return "A match is already in progress.";
        }
        if (finaleLocation == null) {
            return "Finale location is not set. Use /hg setfinale first.";
        }

        players.clear();
        assignedSpawns.clear();
        lockedStartLocations.clear();

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
        if (players.isEmpty()) {
            return "No players available to start.";
        }

        if (!assignSpawnsInternal()) {
            return "Not enough configured spawn points across sections.";
        }

        currentMatchSeed = System.currentTimeMillis();

        state = MatchState.LOBBY_COUNTDOWN;
        Bukkit.broadcastMessage(ChatColor.GOLD + "[HG] Games starting in " + config.getLobbyCountdownSeconds() + "s...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            teleportPlayersToAssignedSpawns();
            state = MatchState.SPAWN_LOCK_COUNTDOWN;
            Bukkit.broadcastMessage(ChatColor.YELLOW + "[HG] Spawn lock active for " + config.getSpawnLockCountdownSeconds() + "s.");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                state = MatchState.LIVE;
                lockedStartLocations.clear();
                Bukkit.broadcastMessage(ChatColor.GREEN + "[HG] The Hunger Games have begun.");
            }, config.getSpawnLockCountdownSeconds() * 20L);
        }, config.getLobbyCountdownSeconds() * 20L);
        return null;
    }

    public void stopGames() {
        state = MatchState.FINISHED;
        lockedStartLocations.clear();
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

    public void registerSpawnPoint(String sectionId, String spawnId, Location location) {
        SpawnPoint point = new SpawnPoint(sectionId.toLowerCase(), spawnId.toLowerCase(), location.clone());
        spawnPointsBySection.computeIfAbsent(point.sectionId(), ignored -> new ArrayList<>());

        List<SpawnPoint> points = spawnPointsBySection.get(point.sectionId());
        points.removeIf(existing -> existing.spawnId().equalsIgnoreCase(point.spawnId()));
        points.add(point);
        points.sort(Comparator.comparing(SpawnPoint::spawnId));
        arenaStore.saveSpawnPoint(point);
    }

    public void setFinaleLocation(Location location) {
        this.finaleLocation = location.clone();
        arenaStore.saveFinale(location);
    }

    public boolean assignSpawns() {
        if (players.isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.put(player.getUniqueId(), new PlayerMatchState(
                        player.getUniqueId(),
                        player.getName(),
                        config.getLivesPerPlayer()
                ));
            }
        }
        return assignSpawnsInternal();
    }

    public String previewSpawns() {
        boolean ok = assignSpawns();
        if (!ok) {
            return null;
        }

        List<String> lines = getSpawnAssignmentLines();
        if (lines.isEmpty()) {
            return "No assignments available.";
        }
        return String.join("\n", lines);
    }

    public List<String> getSpawnAssignmentLines() {
        List<String> lines = new ArrayList<>();
        List<Map.Entry<UUID, SpawnPoint>> assignments = new ArrayList<>(assignedSpawns.entrySet());
        assignments.sort(
                Comparator.comparing((Map.Entry<UUID, SpawnPoint> e) -> e.getValue().sectionId())
                        .thenComparing(e -> e.getValue().spawnId())
        );

        for (Map.Entry<UUID, SpawnPoint> entry : assignments) {
            PlayerMatchState state = players.get(entry.getKey());
            if (state == null) {
                continue;
            }
            SpawnPoint point = entry.getValue();
            lines.add(state.getPlayerName() + " -> " + point.sectionId() + "/" + point.spawnId());
        }
        return lines;
    }

    public Map<String, Integer> getSpawnSectionCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String section : config.getSections()) {
            counts.put(section.toLowerCase(), 0);
        }
        for (SpawnPoint point : assignedSpawns.values()) {
            counts.put(point.sectionId(), counts.getOrDefault(point.sectionId(), 0) + 1);
        }
        return counts;
    }

    public List<String> getSpawnAssignmentPage(int page, int pageSize) {
        List<String> lines = getSpawnAssignmentLines();
        if (lines.isEmpty() || pageSize <= 0) {
            return List.of();
        }

        int safePage = Math.max(1, page);
        int start = (safePage - 1) * pageSize;
        if (start >= lines.size()) {
            return List.of();
        }
        int end = Math.min(start + pageSize, lines.size());
        return new ArrayList<>(lines.subList(start, end));
    }

    public int getSpawnAssignmentTotalPages(int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        int total = getSpawnAssignmentLines().size();
        if (total == 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }

    public String exportSpawnPreviewToFile() throws IOException {
        boolean ok = assignSpawns();
        if (!ok) {
            return null;
        }

        List<String> out = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        out.add("Hunger Games Spawn Assignment Export");
        out.add("generated_at_utc=" + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        out.add("player_count=" + registeredCount());
        out.add("");
        out.add("Section totals:");
        for (String section : config.getSections()) {
            int count = getSpawnSectionCounts().getOrDefault(section.toLowerCase(), 0);
            out.add("- " + section.toLowerCase() + ": " + count);
        }
        out.add("");
        out.add("Assignments:");
        for (String line : getSpawnAssignmentLines()) {
            out.add("- " + line);
        }

        Path reportsDir = plugin.getDataFolder().toPath().resolve("reports");
        Files.createDirectories(reportsDir);
        String fileName = "spawn-preview-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt";
        Path output = reportsDir.resolve(fileName);
        Files.write(output, out);
        return output.toAbsolutePath().toString();
    }

    public Location getAssignedRespawnLocation(UUID playerId) {
        SpawnPoint point = assignedSpawns.get(playerId);
        if (point == null) {
            return null;
        }
        return point.location().clone();
    }

    public Location getLockedStartLocation(UUID playerId) {
        Location loc = lockedStartLocations.get(playerId);
        return loc == null ? null : loc.clone();
    }

    public int registeredCount() {
        return players.size();
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
            Player winner = resolveWinner();
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "[HG] Winner: " + winner.getName());
                if (finaleLocation != null) {
                    winner.teleport(finaleLocation);
                }
            }
            state = MatchState.FINISHED;
        }
    }

    private Player resolveWinner() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerMatchState state = players.get(online.getUniqueId());
            if (state != null && !state.isEliminated()) {
                return online;
            }
        }
        return null;
    }

    private void teleportPlayersToAssignedSpawns() {
        for (Map.Entry<UUID, SpawnPoint> entry : assignedSpawns.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            Location destination = entry.getValue().location().clone();
            player.teleport(destination);
            lockedStartLocations.put(entry.getKey(), destination);
        }
    }

    private boolean assignSpawnsInternal() {
        assignedSpawns.clear();
        List<String> sections = config.getSections();
        if (sections.isEmpty() || players.isEmpty()) {
            return false;
        }

        Map<String, List<SpawnPoint>> pools = new HashMap<>();
        int totalSpawns = 0;
        for (String section : sections) {
            List<SpawnPoint> points = new ArrayList<>(spawnPointsBySection.getOrDefault(section.toLowerCase(), List.of()));
            if (!points.isEmpty()) {
                pools.put(section.toLowerCase(), points);
                totalSpawns += points.size();
            }
        }

        if (totalSpawns < players.size()) {
            return false;
        }

        Random random = new Random(currentMatchSeed);
        List<PlayerMatchState> roster = new ArrayList<>(players.values());
        Collections.shuffle(roster, random);

        int sectionCount = sections.size();
        int base = roster.size() / sectionCount;
        int remainder = roster.size() % sectionCount;

        int rosterIndex = 0;
        for (int i = 0; i < sectionCount; i++) {
            String section = sections.get(i).toLowerCase();
            int target = base + (i < remainder ? 1 : 0);
            if (target == 0) {
                continue;
            }

            List<SpawnPoint> points = pools.getOrDefault(section, new ArrayList<>());
            Collections.shuffle(points, random);
            if (points.size() < target) {
                return false;
            }

            for (int j = 0; j < target; j++) {
                PlayerMatchState playerState = roster.get(rosterIndex++);
                SpawnPoint point = points.get(j);
                playerState.setSectionId(section);
                playerState.setSpawnId(point.spawnId());
                assignedSpawns.put(playerState.getUuid(), point);
            }
        }

        return assignedSpawns.size() == players.size();
    }
}
