package com.btree.hungergames.listener;

import com.btree.hungergames.loot.LootService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public final class LootChestListener implements Listener {
    private final LootService lootService;

    public LootChestListener(LootService lootService) {
        this.lootService = lootService;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!lootService.isLootChestToken(item)) {
            return;
        }

        Block block = event.getBlockPlaced();
        if (block.getType() != Material.CHEST) {
            return;
        }

        if (!(block.getState() instanceof Chest chest)) {
            return;
        }

        String tierType = lootService.getTierType(item);
        int tierLevel = lootService.getTierLevel(item);
        if (tierType == null || tierLevel < 1) {
            return;
        }

        lootService.fillInventory(chest.getBlockInventory(), tierType, tierLevel);
        chest.update();
    }
}
