package com.btree.hungergames.command;

import com.btree.hungergames.loot.LootService;
import com.btree.hungergames.match.MatchManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HGCommand implements CommandExecutor, TabCompleter {
    private final MatchManager matchManager;
    private final LootService lootService;

    public HGCommand(MatchManager matchManager, LootService lootService) {
        this.matchManager = matchManager;
        this.lootService = lootService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /hg <startgames|stop|status|setlives|registerspawn|setfinale|assignspawns|previewspawns|givechest>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "startgames" -> {
                if (!sender.hasPermission("hg.admin.start")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                String error = matchManager.startGames();
                if (error != null) {
                    sender.sendMessage(ChatColor.RED + error);
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Started Hunger Games flow.");
                return true;
            }
            case "stop" -> {
                if (!sender.hasPermission("hg.admin.stop")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                matchManager.stopGames();
                sender.sendMessage(ChatColor.GREEN + "Stopped current match.");
                return true;
            }
            case "status" -> {
                if (!sender.hasPermission("hg.admin.status")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                sender.sendMessage(ChatColor.AQUA + "State: " + matchManager.getState());
                sender.sendMessage(ChatColor.AQUA + "Alive: " + matchManager.aliveCount());
                sender.sendMessage(ChatColor.AQUA + "Eliminated: " + matchManager.eliminatedCount());
                return true;
            }
            case "setlives" -> {
                if (!sender.hasPermission("hg.admin.moderate")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /hg setlives <player> <value>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                int value;
                try {
                    value = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Lives must be a number.");
                    return true;
                }
                matchManager.setLives(target, value);
                sender.sendMessage(ChatColor.GREEN + "Updated lives for " + target.getName() + " to " + value + ".");
                return true;
            }
            case "registerspawn" -> {
                if (!sender.hasPermission("hg.admin.setup")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can register spawn points.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /hg registerspawn <section> <spawn-id>");
                    return true;
                }
                String section = args[1].toLowerCase();
                String spawnId = args[2].toLowerCase();
                matchManager.registerSpawnPoint(section, spawnId, player.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Registered spawn " + spawnId + " in section " + section + ".");
                return true;
            }
            case "setfinale" -> {
                if (!sender.hasPermission("hg.admin.setup")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can set finale location.");
                    return true;
                }
                matchManager.setFinaleLocation(player.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Finale location updated.");
                return true;
            }
            case "assignspawns" -> {
                if (!sender.hasPermission("hg.admin.setup")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                boolean ok = matchManager.assignSpawns();
                if (!ok) {
                    sender.sendMessage(ChatColor.RED + "Spawn assignment failed. Check section spawn counts.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Spawn assignment successful for " + matchManager.registeredCount() + " players.");
                return true;
            }
            case "previewspawns" -> {
                if (!sender.hasPermission("hg.admin.setup")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }

                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Page must be a number.");
                        return true;
                    }
                }

                boolean ok = matchManager.assignSpawns();
                if (!ok) {
                    sender.sendMessage(ChatColor.RED + "Spawn preview failed. Check section spawn counts.");
                    return true;
                }

                final int pageSize = 15;
                int totalPages = matchManager.getSpawnAssignmentTotalPages(pageSize);
                if (page < 1 || page > Math.max(1, totalPages)) {
                    sender.sendMessage(ChatColor.RED + "Page out of range. Valid pages: 1 to " + Math.max(1, totalPages));
                    return true;
                }

                Map<String, Integer> sectionCounts = matchManager.getSpawnSectionCounts();
                sender.sendMessage(ChatColor.AQUA + "Spawn section totals:");
                for (Map.Entry<String, Integer> entry : sectionCounts.entrySet()) {
                    sender.sendMessage(ChatColor.GRAY + " - " + entry.getKey() + ": " + entry.getValue());
                }

                List<String> lines = matchManager.getSpawnAssignmentPage(page, pageSize);
                sender.sendMessage(ChatColor.AQUA + "Spawn assignments page " + page + "/" + Math.max(1, totalPages) + ":");
                for (String line : lines) {
                    sender.sendMessage(ChatColor.GRAY + " - " + line);
                }
                return true;
            }
            case "givechest" -> {
                if (!sender.hasPermission("hg.admin.loot")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can receive loot chest items.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /hg givechest <normal|copper> <tier>");
                    return true;
                }

                String type = args[1].toLowerCase();
                if (!"normal".equals(type) && !"copper".equals(type)) {
                    sender.sendMessage(ChatColor.RED + "Type must be normal or copper.");
                    return true;
                }
                int tier;
                try {
                    tier = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Tier must be a number.");
                    return true;
                }
                player.getInventory().addItem(lootService.createChestToken(type, tier));
                sender.sendMessage(ChatColor.GREEN + "Given " + type + " chest token tier " + tier + ".");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Unknown subcommand.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("startgames", "stop", "status", "setlives", "registerspawn", "setfinale", "assignspawns", "previewspawns", "givechest");
        }
        if (args.length == 2 && "setlives".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && "givechest".equalsIgnoreCase(args[0])) {
            return List.of("normal", "copper");
        }
        if (args.length == 2 && "registerspawn".equalsIgnoreCase(args[0])) {
            return List.of("red", "orange", "blue", "purple");
        }
        if (args.length == 2 && "previewspawns".equalsIgnoreCase(args[0])) {
            return List.of("1", "2", "3", "4", "5");
        }
        if (args.length == 3 && "givechest".equalsIgnoreCase(args[0])) {
            return List.of("1", "2", "3", "4");
        }
        return new ArrayList<>();
    }
}
