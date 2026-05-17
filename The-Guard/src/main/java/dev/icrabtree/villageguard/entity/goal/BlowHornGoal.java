package dev.icrabtree.villageguard.entity.goal;

import dev.icrabtree.villageguard.entity.GuardEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Makes a guard blow the "Ponder" goat horn during combat — once on entry
 * (handled by GuardEntity.setTarget) and then every 1–2 minutes while
 * a target is held.
 */
public class BlowHornGoal extends Goal {

    private static final int MIN_INTERVAL  = 1200; // 60 s (1 minute)
    private static final int INTERVAL_RANGE = 1200; // +0–60 s  →  1–2 minutes total

    private final GuardEntity guard;
    private int cooldown = 0; // starts at 0 so first periodic blow fires quickly after combat entry

    public BlowHornGoal(GuardEntity guard) {
        this.guard = guard;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (guard.getTarget() == null) return false;
        if (--cooldown > 0) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // fires once per activation, cooldown resets in start()
    }

    @Override
    public void start() {
        // Always "Ponder" (index 0) — the classic village rally horn
        guard.level().playSound(null,
                guard.getX(), guard.getY(), guard.getZ(),
                SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0),
                SoundSource.NEUTRAL, 1.4f, 1.0f);
        cooldown = MIN_INTERVAL + guard.getRandom().nextInt(INTERVAL_RANGE);
    }
}
