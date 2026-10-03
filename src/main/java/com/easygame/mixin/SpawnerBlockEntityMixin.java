package com.easygame.mixin;

import com.easygame.generator.GeneratorStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerBlockEntityMixin {

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onServerTick(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity blockEntity, CallbackInfo ci) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            GeneratorStateManager.INSTANCE.tick(serverLevel, pos);
        }
    }
}
