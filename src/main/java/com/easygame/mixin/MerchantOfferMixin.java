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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    @Shadow
    public abstract ItemStack getResult();

    /**
     * Requirement: Unlimited trades (villagers never run out of stock / no trade limits or caps).
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

    @Inject(method = "hasCostB", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHasCostB(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Requirement: Fletcher Trade Customization:
     * Sell: 1 Stick -> 1 Emerald (overrides 32 sticks -> 1 emerald).
     */
    @Inject(method = "getBaseCostA", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetBaseCostA(CallbackInfoReturnable<ItemCost> cir) {
        ItemCost original = cir.getReturnValue();
        if (original != null && original.item().value() == Items.STICK && this.getResult().is(Items.EMERALD)) {
            cir.setReturnValue(new ItemCost(Items.STICK, 1));
        }
    }

    @Inject(method = "getCostA", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetCostA(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack original = cir.getReturnValue();
        if (original != null && original.is(Items.STICK) && this.getResult().is(Items.EMERALD)) {
            ItemStack singleStick = original.copy();
            singleStick.setCount(1);
            cir.setReturnValue(singleStick);
        }
    }

    @Inject(method = "getItemCostA", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetItemCostA(CallbackInfoReturnable<ItemCost> cir) {
        ItemCost original = cir.getReturnValue();
        if (original != null && original.item().value() == Items.STICK && this.getResult().is(Items.EMERALD)) {
            cir.setReturnValue(new ItemCost(Items.STICK, 1));
        }
    }

    /**
     * Requirement: Trade XP Override:
     * Set vanilla trade XP drops to 0 so the 3 full levels given on trade take strictly overrides default XP.
     */
    @Inject(method = "getXp", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetXp(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
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
