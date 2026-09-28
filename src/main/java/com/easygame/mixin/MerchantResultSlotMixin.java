package com.easygame.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public class MerchantResultSlotMixin {

    /**
     * Requirement: Every single trade gives 3 full bars/levels of XP all the time.
     */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTakeTradeResult(Player player, ItemStack stack, CallbackInfo ci) {
        if (!player.level().isClientSide()) {
            player.giveExperienceLevels(3);
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f
            );
        }
    }
}
