package dev.icrabtree.villageguard.client.renderer;

import dev.icrabtree.villageguard.entity.GuardVariant;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Holds per-frame render data extracted from GuardEntity (variant controls which texture to use). */
public class GuardRenderState extends HumanoidRenderState {
    public GuardVariant variant = GuardVariant.SCOUT;
}
