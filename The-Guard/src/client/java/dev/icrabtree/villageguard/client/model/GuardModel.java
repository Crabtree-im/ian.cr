package dev.icrabtree.villageguard.client.model;

import dev.icrabtree.villageguard.client.renderer.GuardRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Guard body model — standard humanoid proportions (same UV layout as a player skin, 64×64).
 * Swap the texture PNGs in assets/village-guard/textures/entity/ for each variant.
 */
public class GuardModel extends HumanoidModel<GuardRenderState> {

    public GuardModel(ModelPart root) {
        super(root);
    }

    /** Creates the layer definition for the guard's main body (skin, 64×64). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        // Match the vanilla villager nose silhouette and mount it one pixel higher on the face.
        // UV: texOffs(24, 0) — occupies (24-30, 0-5), a safe unused region in the 64×64 head UV.
        // Box: 2 wide × 4 tall × 2 deep, protruding 2 units out from the face front (z=-4 → z=-6).
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("nose",
            CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Outer armor layer (helmet, chestplate, boots) — inflated 1.0f so armor sits above the body. */
    public static LayerDefinition createOuterArmorLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0f), 0.0f);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Inner armor layer (leggings) — inflated 0.5f so leggings sit above the body but under outer armor. */
    public static LayerDefinition createInnerArmorLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.5f), 0.0f);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
