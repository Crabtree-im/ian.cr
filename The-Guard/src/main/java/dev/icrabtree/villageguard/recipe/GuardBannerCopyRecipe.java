package dev.icrabtree.villageguard.recipe;

import dev.icrabtree.villageguard.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 3x3 recipe:
 *  E E E
 *  E B E   (B = any banner, including patterns/colors)
 *  E E E   (E = emerald block)
 *
 * Output keeps the banner's existing visual data while changing the item type
 * to village-guard:guard_banner.
 */
public class GuardBannerCopyRecipe extends CustomRecipe {

    public GuardBannerCopyRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack stack = input.getItem(row * 3 + col);
                boolean center = (row == 1 && col == 1);

                if (center) {
                    if (!stack.is(ItemTags.BANNERS)) return false;
                } else {
                    if (!stack.is(Items.EMERALD_BLOCK)) return false;
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack banner = input.getItem(4);
        if (banner.isEmpty() || !banner.is(ItemTags.BANNERS)) return ItemStack.EMPTY;

        // Preserve banner appearance/components and transmute only the item type.
        ItemStack out = banner.transmuteCopy(ModItems.GUARD_BANNER, 1);
        out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return out;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.GUARD_BANNER_COPY;
    }
}
