package dev.icrabtree.villageguard.client.renderer;

import com.google.common.collect.ImmutableMap;
import dev.icrabtree.villageguard.client.model.GuardModel;
import dev.icrabtree.villageguard.client.model.GuardModelLayers;
import dev.icrabtree.villageguard.entity.GuardEntity;
import dev.icrabtree.villageguard.entity.GuardVariant;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class GuardRenderer extends HumanoidMobRenderer<GuardEntity, GuardRenderState, GuardModel> {

    /**
     * One texture per visual variant.
     * Place 64×64 player-skin-format PNGs at these paths inside src/main/resources:
     *   assets/village-guard/textures/entity/guard_scout.png
     *   assets/village-guard/textures/entity/guard_soldier.png
     *   assets/village-guard/textures/entity/guard_captain.png
     */
    private static final Map<GuardVariant, Identifier> TEXTURES = ImmutableMap.of(
            GuardVariant.SCOUT,   Identifier.fromNamespaceAndPath("village-guard", "textures/entity/guard_scout.png"),
            GuardVariant.SOLDIER, Identifier.fromNamespaceAndPath("village-guard", "textures/entity/guard_soldier.png"),
            GuardVariant.CAPTAIN, Identifier.fromNamespaceAndPath("village-guard", "textures/entity/guard_captain.png")
    );

    public GuardRenderer(EntityRendererProvider.Context context) {
        super(context,
                new GuardModel(context.bakeLayer(GuardModelLayers.GUARD)),
                0.5f);

        // Bake armor models and add the armor render layer
        ArmorModelSet<GuardModel> armorModels = ArmorModelSet.bake(
                GuardModelLayers.GUARD_ARMOR_SET,
                context.getModelSet(),
                GuardModel::new
        );
        this.addLayer(new HumanoidArmorLayer<>(this, armorModels, context.getEquipmentRenderer()));
    }

    @Override
    public GuardRenderState createRenderState() {
        return new GuardRenderState();
    }

    @Override
    public void extractRenderState(GuardEntity entity, GuardRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.variant = entity.getVariant();
    }

    @Override
    public Identifier getTextureLocation(GuardRenderState state) {
        return TEXTURES.getOrDefault(state.variant, TEXTURES.get(GuardVariant.SCOUT));
    }
}
