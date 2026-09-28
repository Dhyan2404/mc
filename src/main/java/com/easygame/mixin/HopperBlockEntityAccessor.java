package com.easygame.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public interface HopperBlockEntityAccessor {

    @Invoker("setCooldown")
    void easygame$setCooldown(int cooldown);

    @Invoker("tryMoveItems")
    static boolean easygame$tryMoveItems(Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper, BooleanSupplier validator) {
        throw new UnsupportedOperationException();
    }
}
