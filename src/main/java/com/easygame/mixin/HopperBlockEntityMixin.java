package com.easygame.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(method = "pushItemsTick", at = @At("RETURN"), require = 0)
    private static void onPushItemsTickReturn(Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper, CallbackInfo ci) {
        if (level.isClientSide()) return;

        Component customName = hopper.getCustomName();
        if (customName == null) return;

        String name = customName.getString().toLowerCase();
        boolean isEasyHopper = name.contains("easy hopper") || name.contains("easyhopper") || name.contains("insta hopper");
        if (!isEasyHopper) return;

        ((HopperBlockEntityAccessor) hopper).easygame$setCooldown(0);

        for (int i = 0; i < 63; i++) {
            boolean moved = HopperBlockEntityAccessor.easygame$tryMoveItems(
                    level,
                    pos,
                    state,
                    hopper,
                    () -> HopperBlockEntity.suckInItems(level, hopper)
            );
            ((HopperBlockEntityAccessor) hopper).easygame$setCooldown(0);
            if (!moved) {
                break;
            }
        }
    }
}
