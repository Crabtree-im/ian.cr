package dev.icrabtree.villageguard.init;

import dev.icrabtree.villageguard.VillageGuard;
import dev.icrabtree.villageguard.entity.GuardEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    private static final ResourceKey<EntityType<?>> GUARD_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(VillageGuard.MOD_ID, "guard")
    );

    public static final EntityType<GuardEntity> GUARD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            GUARD_KEY,
            EntityType.Builder.of(GuardEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build(GUARD_KEY)
    );

    /** Called from VillageGuard#onInitialize to trigger static class loading. */
    public static void register() {
        VillageGuard.LOGGER.info("Registering Village Guard entity types");
    }
}
