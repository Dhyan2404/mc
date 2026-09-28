package com.easygame.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    @Final
    private DataSlot cost;

    /**
     * Requirement: Always exactly 1 level of XP needed for anvil combining/repairing/naming,
     * no extra XP, never "Too Expensive!".
     */
    @Inject(method = "createResult", at = @At("RETURN"), require = 0)
    private void onCreateResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty()) {
            // Set XP cost strictly to 1 level
            this.cost.set(1);
            // Clear repair cost penalty so it stays 1 level forever
            output.set(DataComponents.REPAIR_COST, 0);
        }
    }

    /**
     * Ensure the player can take the item as long as they have at least 1 level of XP.
     */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMayPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty()) {
            cir.setReturnValue(player.getAbilities().instabuild || player.experienceLevel >= 1);
        }
    }
}
