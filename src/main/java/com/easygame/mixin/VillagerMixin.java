package com.easygame.mixin;

import com.easygame.trade.TradeHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public class VillagerMixin {

    @Inject(method = "updateTrades", at = @At("RETURN"), require = 0)
    private void onUpdateTrades(ServerLevel serverLevel, CallbackInfo ci) {
        TradeHelper.customizeVillagerOffers((Villager) (Object) this);
    }

    @Inject(method = "setTradingPlayer", at = @At("HEAD"), require = 0)
    private void onSetTradingPlayer(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (player != null) {
            TradeHelper.customizeVillagerOffers((Villager) (Object) this);
        }
    }

    /**
     * Requirement: No trade limits, no caps.
     * Villagers can always restock and never get capped.
     */
    @Inject(method = "canRestock", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCanRestock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
