package dev.icrabtree.villageguard.recipe;

import com.mojang.serialization.MapCodec;
import dev.icrabtree.villageguard.VillageGuard;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {

    private static final GuardBannerCopyRecipe GUARD_BANNER_RECIPE_INSTANCE = new GuardBannerCopyRecipe();

    public static final RecipeSerializer<GuardBannerCopyRecipe> GUARD_BANNER_COPY = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(VillageGuard.MOD_ID, "guard_banner_copy"),
        new RecipeSerializer<>(
            MapCodec.unit(GUARD_BANNER_RECIPE_INSTANCE),
            StreamCodec.unit(GUARD_BANNER_RECIPE_INSTANCE).cast()
        )
    );

    public static void register() {
        VillageGuard.LOGGER.info("Registering Village Guard recipes");
    }
}
