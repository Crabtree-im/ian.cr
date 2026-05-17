package dev.icrabtree.villageguard;

import dev.icrabtree.villageguard.entity.GuardEntity;
import dev.icrabtree.villageguard.init.ModEntities;
import dev.icrabtree.villageguard.init.ModItems;
import dev.icrabtree.villageguard.recipe.ModRecipes;
import dev.icrabtree.villageguard.spawner.VillageWaveSpawner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageGuard implements ModInitializer {

    public static final String MOD_ID = "village-guard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();
                ModRecipes.register();
        FabricDefaultAttributeRegistry.register(ModEntities.GUARD, GuardEntity.createAttributes());

        // Village wave spawning — every 20 min, spawn a wave if all prior guards were killed
        VillageWaveSpawner.register();

        // Natural village spawning — guards appear in all 5 vanilla village biomes
        // weight=5 (rare but present); group size=1 because finalizeSpawn spawns 5–8 companions
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS,
                        Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA,
                        Biomes.DESERT,
                        Biomes.SNOWY_PLAINS,
                        Biomes.TAIGA, Biomes.SNOWY_TAIGA
                ),
                MobCategory.CREATURE,
                ModEntities.GUARD,
                5, 1, 1
        );

        // Add spawn egg to the creative Spawn Eggs tab
        ResourceKey<CreativeModeTab> spawnEggsTab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", "spawn_eggs")
        );
        CreativeModeTabEvents.modifyOutputEvent(spawnEggsTab)
                .register(output -> output.accept(
                        new ItemStack(ModItems.GUARD_SPAWN_EGG),
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                ));

        // Add guard banner to the Combat tab
        ResourceKey<CreativeModeTab> combatTab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", "combat")
        );
        CreativeModeTabEvents.modifyOutputEvent(combatTab)
                .register(output -> output.accept(
                        new ItemStack(ModItems.GUARD_BANNER),
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                ));

        LOGGER.info("Village Guard loaded");
    }
}
