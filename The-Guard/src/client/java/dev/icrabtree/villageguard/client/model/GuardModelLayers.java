package dev.icrabtree.villageguard.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.resources.Identifier;

public class GuardModelLayers {

    private static final Identifier GUARD_ID = Identifier.fromNamespaceAndPath("village-guard", "guard");

    /** Main body/skin layer. */
    public static final ModelLayerLocation GUARD = new ModelLayerLocation(GUARD_ID, "main");

    /** Outer armor layer (helmet, chestplate, boots) — inflated 1.0f. */
    public static final ModelLayerLocation GUARD_ARMOR_OUTER = new ModelLayerLocation(GUARD_ID, "armor_outer");

    /** Inner armor layer (leggings) — inflated 0.5f. */
    public static final ModelLayerLocation GUARD_ARMOR_INNER = new ModelLayerLocation(GUARD_ID, "armor_inner");

    /**
     * ArmorModelSet mapping each armor slot to the correct inflated layer:
     * head/chest/feet → outer (1.0f), legs → inner (0.5f).
     */
    public static final ArmorModelSet<ModelLayerLocation> GUARD_ARMOR_SET =
            new ArmorModelSet<>(GUARD_ARMOR_OUTER, GUARD_ARMOR_OUTER, GUARD_ARMOR_INNER, GUARD_ARMOR_OUTER);
}
