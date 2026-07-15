package com.btree.hungergames.listener;

import com.btree.hungergames.loot.LootService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
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
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.CHEST) {
            return;
        }
        if (!(block.getState() instanceof Chest chest)) {
            return;
        }

        Player player = event.getPlayer();

        // Fill mode: any chest placed by an admin gets filled with random loot.
        if (lootService.isFillModeActive() && player.hasPermission("hg.admin.loot")) {
            String tier = lootService.fillInventoryRandom(chest.getBlockInventory());
            chest.update();
            if (tier != null) {
                player.sendMessage(ChatColor.GREEN + "[HG] Chest filled: " + tier);
            }
            return;
        }

        // Legacy token: fill a specific tier from a givechest token item.
        ItemStack item = event.getItemInHand();
        if (!lootService.isLootChestToken(item)) {
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
