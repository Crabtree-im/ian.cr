package com.btree.hungergames.command;

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

public final class HGCommand implements CommandExecutor, TabCompleter {
    private final MatchManager matchManager;

    public HGCommand(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /hg <startgames|stop|status|setlives>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "startgames" -> {
                if (!sender.hasPermission("hg.admin.start")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                matchManager.startGames();
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
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Unknown subcommand.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("startgames", "stop", "status", "setlives");
        }
        if (args.length == 2 && "setlives".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return new ArrayList<>();
    }
}
