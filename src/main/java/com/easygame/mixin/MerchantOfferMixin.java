package com.easygame.mixin;

import com.easygame.trade.TradeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    /**
     * Requirement: Unlimited trades (villagers never run out of stock / no trade limits or caps).
     */

    @Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true, require = 0)
    private void onIsOutOfStock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "needsRestock", at = @At("HEAD"), cancellable = true, require = 0)
    private void onNeedsRestock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "getUses", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetUses(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "increaseUses", at = @At("HEAD"), cancellable = true, require = 0)
    private void onIncreaseUses(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "setToOutOfStock", at = @At("HEAD"), cancellable = true, require = 0)
    private void onSetToOutOfStock(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getMaxUses", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetMaxUses(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(999999);
    }

    @Inject(method = "getDemand", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetDemand(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "updateDemand", at = @At("HEAD"), cancellable = true, require = 0)
    private void onUpdateDemand(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Requirement: Only max level of enchantment trades (Sharpness 5, Unbreaking 3, etc.).
     * Maximized upon assembly when taking trade output.
     */
    @Inject(method = "assemble", at = @At("RETURN"), require = 0)
    private void onAssemble(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack != null && !stack.isEmpty()) {
            TradeHelper.maximizeEnchantments(stack);
        }
    }
}
