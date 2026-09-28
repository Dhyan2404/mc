package com.easygame.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    public AnvilMenuMixin(MenuType<?> menuType, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(menuType, containerId, playerInventory, access);
    }

    /**
     * Requirement: Always exactly 1 level of XP needed for anvil combining/repairing/naming,
     * no extra XP, never "Too Expensive!".
     */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void onCreateResult(CallbackInfo ci) {
        ItemStack output = this.resultSlots.getItem(0);
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
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void onMayPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        if (!this.resultSlots.getItem(0).isEmpty()) {
            cir.setReturnValue(player.getAbilities().instabuild || player.experienceLevel >= 1);
        }
    }
}
