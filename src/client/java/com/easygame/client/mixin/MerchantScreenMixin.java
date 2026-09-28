package com.easygame.client.mixin;

import com.easygame.client.EasyGameClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    @Shadow
    int shopItem;

    @Unique
    private Button saveTradeButton;

    public MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void onInit(CallbackInfo ci) {
        // Cycle Trades Button: refreshes trades without breaking lectern/workstation
        Button cycleButton = Button.builder(Component.literal("⟳ Cycle"), btn -> {
            EasyGameClient.INSTANCE.cycleTrades();
        }).bounds(this.leftPos + 4, this.topPos - 22, 60, 20).build();
        this.addRenderableWidget(cycleButton);

        // Lock / Save Trade Button: marks current trade so it is preserved when cycling
        this.saveTradeButton = Button.builder(Component.literal("☆ Save"), btn -> {
            boolean locked = EasyGameClient.INSTANCE.toggleLock(this.shopItem);
            btn.setMessage(Component.literal(locked ? "★ Saved" : "☆ Save"));
        }).bounds(this.leftPos + 68, this.topPos - 22, 70, 20).build();
        this.addRenderableWidget(this.saveTradeButton);

        // Armor Trims Button: Toolsmith trades all types of armor trims for 2 diamonds each
        Button trimButton = Button.builder(Component.literal("🛡 Trims"), btn -> {
            EasyGameClient.INSTANCE.requestArmorTrims();
        }).bounds(this.leftPos + 142, this.topPos - 22, 65, 20).build();
        this.addRenderableWidget(trimButton);
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"), require = 0)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.saveTradeButton != null) {
            boolean isCurrentLocked = EasyGameClient.INSTANCE.isLocked(this.shopItem);
            this.saveTradeButton.setMessage(Component.literal(isCurrentLocked ? "★ Saved" : "☆ Save"));
        }
    }
}
