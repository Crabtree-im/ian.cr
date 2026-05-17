package dev.icrabtree.villageguard.entity.goal;

import dev.icrabtree.villageguard.entity.GuardEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Makes the guard target any player who recently hit a nearby villager.
 * Uses LivingEntity#getLastHurtByMob() so no event listener is needed.
 */
public class GuardRetaliateGoal extends Goal {

    private final GuardEntity guard;
    private final double scanRadius;
    private LivingEntity targetPlayer;
    private int cooldown;

    public GuardRetaliateGoal(GuardEntity guard, double scanRadius) {
        this.guard = guard;
        this.scanRadius = scanRadius;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        AABB searchBox = this.guard.getBoundingBox().inflate(scanRadius);
        List<AbstractVillager> nearby = this.guard.level()
                .getEntitiesOfClass(AbstractVillager.class, searchBox);

        for (AbstractVillager villager : nearby) {
            LivingEntity attacker = villager.getLastHurtByMob();
            if (attacker instanceof Player player
                    && !player.isCreative()
                    && !player.equals(this.guard.getTarget())) {
                targetPlayer = player;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return targetPlayer != null && targetPlayer.isAlive();
    }

    @Override
    public void start() {
        this.guard.setTarget(targetPlayer);
        cooldown = 60; // 3-second cooldown before re-scanning villagers
    }

    @Override
    public void stop() {
        targetPlayer = null;
    }
}
