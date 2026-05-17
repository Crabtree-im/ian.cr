package dev.icrabtree.villageguard.spawner;

import dev.icrabtree.villageguard.entity.GuardEntity;
import dev.icrabtree.villageguard.init.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ticks once per server tick; every {@link #WAVE_INTERVAL} ticks it scans village
 * bells near all online players (via the POI manager) and, if a bell has no living
 * non-banner-bound guards within 128 blocks, spawns a fresh wave of 5–8 guards.
 *
 * A wave only arrives after the previous wave has been completely wiped out.
 */
public class VillageWaveSpawner {

    /** 24 000 ticks ≈ 20 minutes (one full Minecraft day). */
    private static final int WAVE_INTERVAL = 24_000;
    private static int ticker = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticker < WAVE_INTERVAL) return;
            ticker = 0;

            for (ServerLevel level : server.getAllLevels()) {
                spawnWavesInLevel(level);
            }
        });
    }

    private static void spawnWavesInLevel(ServerLevel level) {
        // Bells register as the MEETING POI type — use PoiManager to find them near players
        Set<BlockPos> bellPositions = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            level.getPoiManager()
                    .getInRange(holder -> holder.is(PoiTypes.MEETING),
                            player.blockPosition(), 150, PoiManager.Occupancy.ANY)
                    .map(poi -> poi.getPos().immutable())
                    .forEach(bellPositions::add);
        }

        for (BlockPos bellPos : bellPositions) {
            trySpawnWave(level, bellPos);
        }
    }

    private static void trySpawnWave(ServerLevel level, BlockPos bellPos) {
        AABB searchBox = new AABB(bellPos).inflate(128);

        // Only spawn a new wave when all previous wave guards are gone
        List<GuardEntity> existing = level.getEntitiesOfClass(GuardEntity.class, searchBox,
                g -> !g.isBannerBound());
        if (!existing.isEmpty()) return;

        int waveSize = 5 + level.getRandom().nextInt(4); // 5–8

        for (int i = 0; i < waveSize; i++) {
            GuardEntity guard = ModEntities.GUARD.create(level, EntitySpawnReason.NATURAL);
            if (guard == null) continue;

            // Place guards in a ring 8–24 blocks from the bell
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double dist = 8 + level.getRandom().nextDouble() * 16;
            double x = bellPos.getX() + 0.5 + Math.cos(angle) * dist;
            double z = bellPos.getZ() + 0.5 + Math.sin(angle) * dist;

            // Walk down up to 10 blocks to find solid ground
            BlockPos spawnPos = BlockPos.containing(x, bellPos.getY(), z);
            for (int dy = 0; dy < 10; dy++) {
                if (!level.getBlockState(spawnPos.below()).isAir()) break;
                spawnPos = spawnPos.below();
            }

            guard.setPos(x, spawnPos.getY(), z);
            guard.setHomeTo(bellPos, 128);
            guard.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                    EntitySpawnReason.NATURAL, null);
            level.addFreshEntity(guard);
        }
    }
}

