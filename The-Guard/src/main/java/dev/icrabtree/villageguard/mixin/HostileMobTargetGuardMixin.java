package dev.icrabtree.villageguard.mixin;

import dev.icrabtree.villageguard.entity.GuardEntity;
import dev.icrabtree.villageguard.init.ModEntities;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the listed hostile mob types proactively target GuardEntity.
 * Each class in @Mixin has registerGoals() defined, so the TAIL inject fires
 * for each of them (and, for abstract bases like AbstractIllager/AbstractSkeleton,
 * also for all subclasses that call super.registerGoals()).
 */
@Mixin({
        Zombie.class,
        AbstractSkeleton.class,
        AbstractIllager.class,
        Witch.class,
        Ravager.class,
        Creeper.class,
        Spider.class,
        Phantom.class,
        Breeze.class
})
public abstract class HostileMobTargetGuardMixin extends net.minecraft.world.entity.monster.Monster {

    // Required synthetic constructor — never called directly
    protected HostileMobTargetGuardMixin(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.monster.Monster> type,
                                          net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void villageGuard$targetGuards(CallbackInfo ci) {
        this.targetSelector.addGoal(3,
                new NearestAttackableTargetGoal<>(this, GuardEntity.class, true));
    }
}
