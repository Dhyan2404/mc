package com.easygame.client.mixin;

import com.easygame.client.render.GeneratorRenderAttachment;
import com.easygame.generator.BlockGenerator;
import com.easygame.mixin.BaseSpawnerAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerRenderer.class)
public class SpawnerRendererMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("RETURN")
    )
    private void onExtractRenderState(
        SpawnerBlockEntity blockEntity,
        SpawnerRenderState state,
        float tickDelta,
        Vec3 cameraPos,
        ModelFeatureRenderer.CrumblingOverlay crumbling,
        CallbackInfo ci
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) return;
        BlockPos pos = blockEntity.getBlockPos();

        GeneratorRenderAttachment attachment = (GeneratorRenderAttachment) state;

        ItemStack renderStack = null;
        int glowColor = 0xFFFFFFFF;

        // 1. Check if configured standalone generator item is set in Spawner nextSpawnData
        BaseSpawner spawner = blockEntity.getSpawner();
        SpawnData spawnData = ((BaseSpawnerAccessor) spawner).getNextSpawnData();
        CompoundTag entityTag = spawnData != null ? spawnData.getEntityToSpawn() : null;

        if (entityTag != null && entityTag.getString("GeneratorItem").isPresent()) {
            String itemId = entityTag.getString("GeneratorItem").get();
            Identifier id = Identifier.tryParse(itemId);
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (item != Items.AIR) {
                    renderStack = new ItemStack(item);
                    BlockGenerator.RarityTier tier = BlockGenerator.INSTANCE.getItemTier(item);
                    glowColor = BlockGenerator.INSTANCE.getTierGlowColor(tier.getTier());
                }
            }
        }

        // 2. Fallback to 8 surrounding blocks ring
        if (renderStack == null) {
            BlockGenerator.SurroundingsResult result = BlockGenerator.INSTANCE.checkSurroundings(level, pos);
            if (result.isActive()) {
                renderStack = new ItemStack(result.getBlock() != null ? result.getBlock() : result.getItem());
                glowColor = BlockGenerator.INSTANCE.getTierGlowColor(result.getTier().getTier());
            }
        }

        if (renderStack != null && !renderStack.isEmpty()) {
            // Null out vanilla mob displayEntity so no pig/zombie renders inside the generator
            state.displayEntity = null;

            attachment.easygame$setGeneratingStack(renderStack);
            attachment.easygame$setGlowColor(glowColor);

            // Smooth continuous 360-degree rotation animation
            long time = System.currentTimeMillis();
            float spin = (time % 3600L) / 10.0f;
            attachment.easygame$setCustomSpin(spin);

            // Ambient client-side glow particles around generator spawner
            if (level.getRandom().nextInt(4) == 0) {
                level.addParticle(
                    ParticleTypes.GLOW,
                    pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8,
                    pos.getY() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8,
                    pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8,
                    0.0, 0.015, 0.0
                );
            }
        } else {
            attachment.easygame$setGeneratingStack(null);
        }
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void onSubmit(
        SpawnerRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector bufferSource,
        CameraRenderState cameraState,
        CallbackInfo ci
    ) {
        GeneratorRenderAttachment attachment = (GeneratorRenderAttachment) state;
        ItemStack stack = attachment.easygame$getGeneratingStack();

        if (stack != null && !stack.isEmpty()) {
            poseStack.pushPose();

            // Center inside the spawner block cage
            poseStack.translate(0.5f, 0.45f, 0.5f);

            // Smooth rotation & subtle dynamic 3D tilt
            poseStack.rotateDegrees(Axis.YP, attachment.easygame$getCustomSpin());
            poseStack.rotateDegrees(Axis.XP, -25.0f);

            // Scale to fit gracefully inside the iron cage
            poseStack.scale(0.55f, 0.55f, 0.55f);

            ItemStackRenderState itemState = new ItemStackRenderState();
            Minecraft mc = Minecraft.getInstance();

            mc.getItemModelResolver().updateForTopItem(
                itemState,
                stack,
                ItemDisplayContext.FIXED,
                mc.level,
                null,
                0
            );

            // Submit at maximum light level (0xF000F0 = light 15 full bright) with colored glow outline
            int glowColor = attachment.easygame$getGlowColor();
            itemState.submit(poseStack, bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY, glowColor);

            poseStack.popPose();
        }
    }
}
