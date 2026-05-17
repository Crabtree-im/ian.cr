package dev.icrabtree.villageguard.init;

import dev.icrabtree.villageguard.VillageGuard;
import dev.icrabtree.villageguard.item.GuardBannerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;

public class ModItems {

    private static final ResourceKey<Item> GUARD_SPAWN_EGG_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(VillageGuard.MOD_ID, "guard_spawn_egg")
    );

    public static final Item GUARD_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            GUARD_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Properties()
                    .setId(GUARD_SPAWN_EGG_KEY)
                    .component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(ModEntities.GUARD, new CompoundTag())))
    );

    private static final ResourceKey<Item> GUARD_BANNER_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(VillageGuard.MOD_ID, "guard_banner")
    );

    public static final Item GUARD_BANNER = Registry.register(
            BuiltInRegistries.ITEM,
            GUARD_BANNER_KEY,
            new GuardBannerItem(new Item.Properties()
                    .setId(GUARD_BANNER_KEY)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static void register() {
        VillageGuard.LOGGER.info("Registering Village Guard items");
    }
}

