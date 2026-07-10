package com.btree.hungergames.loot;

import org.bukkit.Material;

public record LootEntry(String category, Material material, int weight, int min, int max) {
}
