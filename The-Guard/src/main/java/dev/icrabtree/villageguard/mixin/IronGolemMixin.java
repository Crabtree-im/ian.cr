package dev.icrabtree.villageguard.mixin;

import dev.icrabtree.villageguard.entity.GuardEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents Iron Golems from ever targeting or attacking Guards.
 * Guards are village protectors — golems and guards are on the same side.
 */
@Mixin(IronGolem.class)
public abstract class IronGolemMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void villageGuard$noTargetGuards(LivingEntity target, CallbackInfo ci) {
        if (target instanceof GuardEntity) {
            ci.cancel();
        }
    }
}
