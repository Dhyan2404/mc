package com.easygame.client.mixin;

import com.easygame.client.EasyGameClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    @Shadow
    int shopItem;

    @Unique
    private Button saveTradeButton;

    public MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique
    private boolean isToolsmith() {
        Component title = this.getTitle();
        if (title != null) {
            String name = title.getString().toLowerCase();
            if (name.contains("toolsmith")) return true;
            if (title.getContents() instanceof TranslatableContents translatable) {
                return translatable.getKey().toLowerCase().contains("toolsmith");
            }
        }
        return false;
    }

    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void onInit(CallbackInfo ci) {
        // Cycle Trades Button: refreshes trades without breaking lectern/workstation
        Button cycleButton = Button.builder(Component.literal("⟳ Cycle"), btn -> {
            EasyGameClient.INSTANCE.cycleTrades();
        }).bounds(this.leftPos + 4, this.topPos - 22, 60, 20).build();
        this.addRenderableWidget(cycleButton);

        // Lock / Save Trade Button: marks current trade so it is preserved when cycling
        this.saveTradeButton = Button.builder(
                Component.literal(EasyGameClient.INSTANCE.isLocked(this.shopItem) ? "★ Saved" : "☆ Save"),
                btn -> {
                    boolean locked = EasyGameClient.INSTANCE.toggleLock(this.shopItem);
                    btn.setMessage(Component.literal(locked ? "★ Saved" : "☆ Save"));
                }
        ).bounds(this.leftPos + 68, this.topPos - 22, 70, 20).build();
        this.addRenderableWidget(this.saveTradeButton);

        // Armor Trims Button: rendered STRICTLY when interacting with a Toolsmith
        if (this.isToolsmith()) {
            Button trimButton = Button.builder(Component.literal("🛡 Trims"), btn -> {
                EasyGameClient.INSTANCE.requestArmorTrims();
            }).bounds(this.leftPos + 142, this.topPos - 22, 65, 20).build();
            this.addRenderableWidget(trimButton);
        }
    }

    @Inject(method = "containerTick", at = @At("RETURN"), require = 0)
    private void onContainerTick(CallbackInfo ci) {
        if (this.saveTradeButton != null) {
            boolean locked = EasyGameClient.INSTANCE.isLocked(this.shopItem);
            this.saveTradeButton.setMessage(Component.literal(locked ? "★ Saved" : "☆ Save"));
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"), require = 0)
    private void onScreenClose(CallbackInfo ci) {
        EasyGameClient.INSTANCE.clearLocks();
    }
}
