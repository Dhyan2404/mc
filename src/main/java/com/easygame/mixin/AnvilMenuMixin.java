package com.easygame.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("RETURN"), require = 0)
    private void onCreateResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty()) {
            this.cost.set(1);
            output.set(DataComponents.REPAIR_COST, 0);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMayPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty()) {
            cir.setReturnValue(player.getAbilities().instabuild || player.experienceLevel >= 1);
        }
    }
}
