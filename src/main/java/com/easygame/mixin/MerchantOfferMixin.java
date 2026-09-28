package com.easygame.mixin;

import com.easygame.trade.TradeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    @Shadow
    public abstract ItemStack getResult();

    /**
     * Requirement: Unlimited trades (villagers never run out of stock / lock trades).
     */
    @Inject(method = "hasNoUsesLeft", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHasNoUsesLeft(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true, require = 0)
    private void onIsOutOfStock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "needsRestock", at = @At("HEAD"), cancellable = true, require = 0)
    private void onNeedsRestock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "getMaxUses", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetMaxUses(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(999999);
    }

    /**
     * Requirement: Emerald-Only Trades: Remove all secondary item costs from all villager trades
     * (e.g. no normal book requirements).
     */
    @Inject(method = "getItemCostB", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetItemCostB(CallbackInfoReturnable<Optional<ItemCost>> cir) {
        cir.setReturnValue(Optional.empty());
    }

    @Inject(method = "getCostB", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetCostB(CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(ItemStack.EMPTY);
    }

    /**
     * Requirement: Only max level of enchantment trades (Sharpness 5, Unbreaking 3, etc.).
     */
    @Inject(method = "getResult", at = @At("RETURN"), require = 0)
    private void onGetResult(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack != null && !stack.isEmpty()) {
            TradeHelper.maximizeEnchantments(stack);
        }
    }
}
