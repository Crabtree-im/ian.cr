package dev.icrabtree.villageguard.client;

import dev.icrabtree.villageguard.client.model.GuardModel;
import dev.icrabtree.villageguard.client.model.GuardModelLayers;
import dev.icrabtree.villageguard.client.renderer.GuardRenderer;
import dev.icrabtree.villageguard.init.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class VillageGuardClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GUARD, GuardRenderer::new);

        ModelLayerRegistry.registerModelLayer(GuardModelLayers.GUARD, GuardModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(GuardModelLayers.GUARD_ARMOR_OUTER, GuardModel::createOuterArmorLayer);
        ModelLayerRegistry.registerModelLayer(GuardModelLayers.GUARD_ARMOR_INNER, GuardModel::createInnerArmorLayer);
    }
}
