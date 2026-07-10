package com.btree.hungergames.loot;

import org.bukkit.Material;

public record LootEntry(Material material, int weight, int min, int max) {
}
