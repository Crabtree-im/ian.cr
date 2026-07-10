package com.btree.hungergames.config;

import com.btree.hungergames.match.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArenaStore {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public ArenaStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arena.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void saveSpawnPoint(SpawnPoint spawnPoint) {
        String base = "spawns." + spawnPoint.sectionId() + "." + spawnPoint.spawnId();
        Location location = spawnPoint.location();
        yaml.set(base + ".world", location.getWorld() == null ? "world" : location.getWorld().getName());
        yaml.set(base + ".x", location.getX());
        yaml.set(base + ".y", location.getY());
        yaml.set(base + ".z", location.getZ());
        yaml.set(base + ".yaw", location.getYaw());
        yaml.set(base + ".pitch", location.getPitch());
        save();
    }

    public Map<String, List<SpawnPoint>> loadSpawns() {
        Map<String, List<SpawnPoint>> result = new HashMap<>();
        ConfigurationSection sections = yaml.getConfigurationSection("spawns");
        if (sections == null) {
            return result;
        }

        for (String sectionId : sections.getKeys(false)) {
            ConfigurationSection section = sections.getConfigurationSection(sectionId);
            if (section == null) {
                continue;
            }
            List<SpawnPoint> points = new ArrayList<>();
            for (String spawnId : section.getKeys(false)) {
                ConfigurationSection sp = section.getConfigurationSection(spawnId);
                if (sp == null) {
                    continue;
                }
                Location location = readLocation(sp);
                if (location != null) {
                    points.add(new SpawnPoint(sectionId, spawnId, location));
                }
            }
            result.put(sectionId, points);
        }

        return result;
    }

    public void saveFinale(Location location) {
        yaml.set("finale.world", location.getWorld() == null ? "world" : location.getWorld().getName());
        yaml.set("finale.x", location.getX());
        yaml.set("finale.y", location.getY());
        yaml.set("finale.z", location.getZ());
        yaml.set("finale.yaw", location.getYaw());
        yaml.set("finale.pitch", location.getPitch());
        save();
    }

    public Location loadFinale() {
        ConfigurationSection finale = yaml.getConfigurationSection("finale");
        if (finale == null) {
            return null;
        }
        return readLocation(finale);
    }

    private Location readLocation(ConfigurationSection section) {
        String worldName = section.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void save() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save arena.yml: " + e.getMessage());
        }
    }
}
