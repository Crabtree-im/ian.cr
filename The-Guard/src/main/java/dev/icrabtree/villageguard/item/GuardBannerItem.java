package dev.icrabtree.villageguard.item;

import dev.icrabtree.villageguard.entity.GuardEntity;
import dev.icrabtree.villageguard.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Guard Banner — crafted from any banner surrounded by 8 emerald blocks (shapeless recipe).
 *
 * Behaviour:
 *  - Places as a normal vanilla banner block (not a spawn-egg style item).
 *  - After successful placement, spawns one linked guard.
 *  - Guard despawns automatically when its linked banner block is removed.
 *
 * The item always has the enchanted-glint foil to distinguish it from a plain banner.
 */
public class GuardBannerItem extends BannerItem {

    public GuardBannerItem(Properties properties) {
        super(Blocks.WHITE_BANNER, Blocks.WHITE_WALL_BANNER, properties);
    }

    // Always render with the enchanted-glint shader
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // Right-click on a block surface → spawn / respawn the linked guard
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // First perform normal vanilla banner placement behavior
        InteractionResult placedResult = super.useOn(ctx);
        if (!placedResult.consumesAction()) return placedResult;

        Level level = ctx.getLevel();
        if (level.isClientSide()) return placedResult;

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos bannerPos = ctx.getClickedPos().relative(ctx.getClickedFace());

        // Spawn position: one block above the clicked face
        BlockPos spawnPos = bannerPos;

        GuardEntity guard = ModEntities.GUARD.create(serverLevel, EntitySpawnReason.SPAWNER);
        if (guard == null) return InteractionResult.FAIL;

        guard.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        guard.setHomeTo(spawnPos, 64);
        guard.finalizeSpawn(serverLevel,
                serverLevel.getCurrentDifficultyAt(spawnPos),
                EntitySpawnReason.SPAWNER, null);
        guard.setBannerBound(true);
        guard.setBannerAnchorPos(bannerPos);
        serverLevel.addFreshEntity(guard);
        return placedResult;
    }
}
