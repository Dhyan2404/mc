package com.easygame.client.mixin;

import com.easygame.client.EasyGameClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
        Button cycleButton = Button.builder(Component.literal("§a⟳ §fCycle"), btn -> {
            EasyGameClient.INSTANCE.cycleTrades();
        })
        .bounds(this.leftPos + 4, this.topPos - 22, 62, 20)
        .tooltip(Tooltip.create(Component.literal("§aCycle Trades§7: Refreshes villager trades instantly without breaking workstation.")))
        .build();
        this.addRenderableWidget(cycleButton);

        // Lock / Save Trade Button: marks current trade so it is preserved when cycling
        boolean initialLocked = EasyGameClient.INSTANCE.isLocked(this.shopItem);
        this.saveTradeButton = Button.builder(
                Component.literal(initialLocked ? "§6★ §eSaved" : "§7☆ §fSave"),
                btn -> {
                    boolean locked = EasyGameClient.INSTANCE.toggleLock(this.shopItem);
                    btn.setMessage(Component.literal(locked ? "§6★ §eSaved" : "§7☆ §fSave"));
                }
        )
        .bounds(this.leftPos + 70, this.topPos - 22, 68, 20)
        .tooltip(Tooltip.create(Component.literal("§eLock Trade§7: Protects this trade from being replaced during cycling.")))
        .build();
        this.addRenderableWidget(this.saveTradeButton);

        // Armor Trims Button: rendered STRICTLY when interacting with a Toolsmith
        if (this.isToolsmith()) {
            Button trimButton = Button.builder(Component.literal("§b🛡 §fTrims"), btn -> {
                EasyGameClient.INSTANCE.requestArmorTrims();
            })
            .bounds(this.leftPos + 142, this.topPos - 22, 64, 20)
            .tooltip(Tooltip.create(Component.literal("§bArmor Trims§7: Adds all 18 rare Armor Trims to Toolsmith trades.")))
            .build();
            this.addRenderableWidget(trimButton);
        }
    }

    @Unique
    private int lastShopItem = -1;

    @Inject(method = "containerTick", at = @At("RETURN"), require = 0)
    private void onContainerTick(CallbackInfo ci) {
        if (this.saveTradeButton != null && this.shopItem != this.lastShopItem) {
            this.lastShopItem = this.shopItem;
            boolean locked = EasyGameClient.INSTANCE.isLocked(this.shopItem);
            this.saveTradeButton.setMessage(Component.literal(locked ? "§6★ §eSaved" : "§7☆ §fSave"));
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"), require = 0)
    private void onScreenClose(CallbackInfo ci) {
        EasyGameClient.INSTANCE.clearLocks();
    }
}
