package com.easygame.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Breeze.class)
public class BreezeMixin {

    /**
     * Requirement: Breeze drops 10 Ominous Trial Keys on death.
     */
    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        Breeze breeze = (Breeze) (Object) this;
        if (!breeze.level().isClientSide()) {
            breeze.spawnAtLocation(new ItemStack(Items.OMINOUS_TRIAL_KEY, 10));
        }
    }
}
