package com.btree.hungergames.loot;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class LootService {
    private static final String[] TABLE_FILES = {
            "normal-tier-1.yml", "normal-tier-2.yml", "normal-tier-3.yml", "normal-tier-4.yml",
            "copper-tier-1.yml", "copper-tier-2.yml", "copper-tier-3.yml"
    };

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Map<String, LootTable> tables = new HashMap<>();
    private final NamespacedKey tierTypeKey;
    private final NamespacedKey tierLevelKey;
    private boolean fillModeActive = false;

    public LootService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.tierTypeKey = new NamespacedKey(plugin, "hg_tier_type");
        this.tierLevelKey = new NamespacedKey(plugin, "hg_tier_level");
    }

    public void loadTables() {
        File lootDir = new File(plugin.getDataFolder(), "loot-tables");
        if (!lootDir.exists()) {
            lootDir.mkdirs();
        }

        for (String fileName : TABLE_FILES) {
            copyDefaultTableIfMissing(lootDir, fileName);
            File file = new File(lootDir, fileName);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String tier = yaml.getString("tier", "");
            List<Integer> slotRange = yaml.getIntegerList("slot_range");
            int minSlots = slotRange.size() > 0 ? slotRange.get(0) : 4;
            int maxSlots = slotRange.size() > 1 ? slotRange.get(1) : 7;
            List<LootEntry> entries = parseEntries(yaml.getConfigurationSection("items"));
            if (!tier.isBlank() && !entries.isEmpty()) {
                tables.put(tier, new LootTable(minSlots, maxSlots, entries));
            }
        }
        plugin.getLogger().info("Loaded " + tables.size() + " loot tables.");
    }

    public int getTableCount() {
        return tables.size();
    }

    public void setFillMode(boolean active) {
        this.fillModeActive = active;
    }

    public boolean isFillModeActive() {
        return fillModeActive;
    }

    public String fillInventoryRandom(Inventory inventory) {
        if (tables.isEmpty()) {
            return null;
        }
        List<String> keys = new ArrayList<>(tables.keySet());
        String key = keys.get(random.nextInt(keys.size()));
        int lastDash = key.lastIndexOf('-');
        if (lastDash < 0 || lastDash >= key.length() - 1) {
            return null;
        }
        String tierType = key.substring(0, lastDash);
        int tierLevel;
        try {
            tierLevel = Integer.parseInt(key.substring(lastDash + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        fillInventory(inventory, tierType, tierLevel);
        return key;
    }

    public ItemStack createChestToken(String tierType, int tierLevel) {
        ItemStack item = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName("HG " + capitalize(tierType) + " Chest T" + tierLevel);
        meta.getPersistentDataContainer().set(tierTypeKey, PersistentDataType.STRING, tierType.toLowerCase());
        meta.getPersistentDataContainer().set(tierLevelKey, PersistentDataType.INTEGER, tierLevel);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isLootChestToken(ItemStack stack) {
        if (stack == null || stack.getType() != Material.CHEST) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(tierTypeKey, PersistentDataType.STRING)
                && meta.getPersistentDataContainer().has(tierLevelKey, PersistentDataType.INTEGER);
    }

    public String getTierType(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(tierTypeKey, PersistentDataType.STRING);
    }

    public int getTierLevel(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return -1;
        }
        Integer value = meta.getPersistentDataContainer().get(tierLevelKey, PersistentDataType.INTEGER);
        return value == null ? -1 : value;
    }

    public void fillInventory(Inventory inventory, String tierType, int tierLevel) {
        String key = tierType.toLowerCase() + "-" + tierLevel;
        LootTable table = tables.get(key);
        if (table == null) {
            return;
        }

        int toFill = table.getMinSlots();
        if (table.getMaxSlots() > table.getMinSlots()) {
            toFill += random.nextInt(table.getMaxSlots() - table.getMinSlots() + 1);
        }

        List<Integer> freeSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                freeSlots.add(i);
            }
        }

        // Guarantee one combat-oriented entry and one food entry when available.
        int guaranteedPlaced = 0;
        guaranteedPlaced += placeGuaranteed(inventory, freeSlots, table, "weapons_tools", "armor");
        guaranteedPlaced += placeGuaranteed(inventory, freeSlots, table, "food");

        int randomFillCount = Math.max(0, toFill - guaranteedPlaced);
        for (int i = 0; i < randomFillCount && !freeSlots.isEmpty(); i++) {
            LootEntry entry = pickWeighted(table.getEntries());
            if (entry == null) {
                break;
            }
            placeEntry(inventory, freeSlots, entry);
        }
    }

    private void copyDefaultTableIfMissing(File lootDir, String fileName) {
        File out = new File(lootDir, fileName);
        if (out.exists()) {
            return;
        }
        try {
            plugin.saveResource("loot-tables/" + fileName, false);
        } catch (IllegalArgumentException ignored) {
            // Resource missing in jar; expected during local dev if not packaged yet.
        }
    }

    private List<LootEntry> parseEntries(ConfigurationSection itemsSection) {
        List<LootEntry> list = new ArrayList<>();
        if (itemsSection == null) {
            return list;
        }

        for (String category : itemsSection.getKeys(false)) {
            List<Map<?, ?>> rawEntries = itemsSection.getMapList(category);
            for (Map<?, ?> raw : rawEntries) {
                String materialName = String.valueOf(raw.get("material"));
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    continue;
                }
                int weight = asInt(raw.get("weight"), 1);
                int min = Math.max(1, asInt(raw.get("min"), 1));
                int max = Math.max(min, asInt(raw.get("max"), min));
                list.add(new LootEntry(category.toLowerCase(), material, Math.max(1, weight), min, max));
            }
        }

        return list;
    }

    private LootEntry pickWeighted(List<LootEntry> entries) {
        if (entries.isEmpty()) {
            return null;
        }
        int total = 0;
        for (LootEntry entry : entries) {
            total += Math.max(1, entry.weight());
        }
        int roll = random.nextInt(total);
        int current = 0;
        for (LootEntry entry : entries) {
            current += Math.max(1, entry.weight());
            if (roll < current) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private int placeGuaranteed(Inventory inventory, List<Integer> freeSlots, LootTable table, String... categories) {
        for (String category : categories) {
            LootEntry entry = pickWeightedByCategory(table.getEntries(), category);
            if (entry != null && !freeSlots.isEmpty()) {
                placeEntry(inventory, freeSlots, entry);
                return 1;
            }
        }
        return 0;
    }

    private LootEntry pickWeightedByCategory(List<LootEntry> entries, String category) {
        List<LootEntry> filtered = new ArrayList<>();
        for (LootEntry entry : entries) {
            if (entry.category().equalsIgnoreCase(category)) {
                filtered.add(entry);
            }
        }
        return pickWeighted(filtered);
    }

    private void placeEntry(Inventory inventory, List<Integer> freeSlots, LootEntry entry) {
        int amount = entry.min();
        if (entry.max() > entry.min()) {
            amount += random.nextInt(entry.max() - entry.min() + 1);
        }
        ItemStack item = new ItemStack(entry.material(), amount);
        int slotIndex = random.nextInt(freeSlots.size());
        int slot = freeSlots.remove(slotIndex);
        inventory.setItem(slot, item);
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "Chest";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
