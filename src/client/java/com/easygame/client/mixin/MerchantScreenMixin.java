package com.easygame.client.mixin;

import com.easygame.client.EasyGameClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
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
    private int shopItem;

    @Shadow
    private int scrollOff;

    @Unique
    private Button saveTradeButton;

    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
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

    @Inject(method = "init", at = @At("HEAD"))
    private void onInitHead(CallbackInfo ci) {
        // Clear all previous locks when opening a merchant screen
        EasyGameClient.INSTANCE.clearLocks();
    }

    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void onInit(CallbackInfo ci) {
        // Hide the inventory label so the buttons fit cleanly inside the box without text overlap
        this.inventoryLabelY = -1000;

        if (this.isToolsmith()) {
            // Cycle Trades Button inside the box
            Button cycleButton = Button.builder(Component.literal("§a⟳ §fCycle"), btn -> {
                EasyGameClient.INSTANCE.cycleTrades();
            })
            .bounds(this.leftPos + 107, this.topPos + 58, 50, 20)
            .tooltip(Tooltip.create(Component.literal("§aCycle Trades§7: Refreshes villager trades instantly without breaking workstation.")))
            .build();
            this.addRenderableWidget(cycleButton);

            // Lock / Save Trade Button inside the box
            this.saveTradeButton = Button.builder(
                    Component.literal(EasyGameClient.INSTANCE.isLocked(this.shopItem) ? "§6★ §eSaved" : "§7☆ §fSave"),
                    btn -> {
                        MerchantOffers offers = this.menu.getOffers();
                        if (offers != null && this.shopItem >= 0 && this.shopItem < offers.size()) {
                            boolean locked = EasyGameClient.INSTANCE.toggleLock(this.shopItem);
                            btn.setMessage(Component.literal(locked ? "§6★ §eSaved" : "§7☆ §fSave"));
                        }
                    }
            )
            .bounds(this.leftPos + 161, this.topPos + 58, 54, 20)
            .tooltip(Tooltip.create(Component.literal("§eLock Trade§7: Protects this trade from being replaced during cycling.")))
            .build();
            this.addRenderableWidget(this.saveTradeButton);

            // Armor Trims Button inside the box
            Button trimButton = Button.builder(Component.literal("§b🛡 §fTrims"), btn -> {
                EasyGameClient.INSTANCE.requestArmorTrims();
            })
            .bounds(this.leftPos + 219, this.topPos + 58, 49, 20)
            .tooltip(Tooltip.create(Component.literal("§bArmor Trims§7: Adds all 18 rare Armor Trims to Toolsmith trades.")))
            .build();
            this.addRenderableWidget(trimButton);
        } else {
            // Cycle Trades Button positioned inside the box (between trade slots and inventory)
            Button cycleButton = Button.builder(Component.literal("§a⟳ §fCycle"), btn -> {
                EasyGameClient.INSTANCE.cycleTrades();
            })
            .bounds(this.leftPos + 110, this.topPos + 58, 74, 20)
            .tooltip(Tooltip.create(Component.literal("§aCycle Trades§7: Refreshes villager trades instantly without breaking workstation.")))
            .build();
            this.addRenderableWidget(cycleButton);

            // Lock / Save Trade Button positioned inside the box (between trade slots and inventory)
            this.saveTradeButton = Button.builder(
                    Component.literal(EasyGameClient.INSTANCE.isLocked(this.shopItem) ? "§6★ §eSaved" : "§7☆ §fSave"),
                    btn -> {
                        MerchantOffers offers = this.menu.getOffers();
                        if (offers != null && this.shopItem >= 0 && this.shopItem < offers.size()) {
                            boolean locked = EasyGameClient.INSTANCE.toggleLock(this.shopItem);
                            btn.setMessage(Component.literal(locked ? "§6★ §eSaved" : "§7☆ §fSave"));
                        }
                    }
            )
            .bounds(this.leftPos + 188, this.topPos + 58, 80, 20)
            .tooltip(Tooltip.create(Component.literal("§eLock Trade§7: Protects this trade from being replaced during cycling.")))
            .build();
            this.addRenderableWidget(this.saveTradeButton);
        }
    }

    @Inject(method = "postButtonClick", at = @At("RETURN"), require = 0)
    private void onPostButtonClick(CallbackInfo ci) {
        if (this.saveTradeButton != null) {
            boolean locked = EasyGameClient.INSTANCE.isLocked(this.shopItem);
            this.saveTradeButton.setMessage(Component.literal(locked ? "§6★ §eSaved" : "§7☆ §fSave"));
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == 0) { // Left click
            double mouseX = event.x();
            double mouseY = event.y();
            MerchantOffers offers = this.menu.getOffers();
            if (offers != null && !offers.isEmpty()) {
                // If clicking directly in the star toggle area of any trade button in the list
                if (mouseX >= this.leftPos + 70 && mouseX <= this.leftPos + 92) {
                    if (mouseY >= this.topPos + 18 && mouseY < this.topPos + 18 + 7 * 20) {
                        int slot = (int) ((mouseY - (this.topPos + 18)) / 20) + this.scrollOff;
                        if (slot >= 0 && slot < offers.size()) {
                            boolean locked = EasyGameClient.INSTANCE.toggleLock(slot);
                            this.shopItem = slot;
                            if (this.saveTradeButton != null) {
                                this.saveTradeButton.setMessage(Component.literal(locked ? "§6★ §eSaved" : "§7☆ §fSave"));
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "extractContents", at = @At("RETURN"), require = 0)
    private void onExtractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        MerchantOffers offers = this.menu.getOffers();
        if (offers == null || offers.isEmpty()) return;

        // Keep shopItem within valid bounds
        if (this.shopItem < 0 || this.shopItem >= offers.size()) {
            this.shopItem = 0;
        }

        // Dynamically update save button text every frame to strictly reflect currently selected trade
        if (this.saveTradeButton != null) {
            boolean locked = EasyGameClient.INSTANCE.isLocked(this.shopItem);
            this.saveTradeButton.setMessage(Component.literal(locked ? "§6★ §eSaved" : "§7☆ §fSave"));
        }

        // Render star indicator on each trade button in the list
        for (int i = 0; i < 7; i++) {
            int tradeIndex = i + this.scrollOff;
            if (tradeIndex < offers.size()) {
                int buttonY = this.topPos + 18 + (i * 20);
                if (EasyGameClient.INSTANCE.isLocked(tradeIndex)) {
                    extractor.text(this.font, "§6★", this.leftPos + 78, buttonY + 6, 0xFFFFD700, true);
                } else if (mouseX >= this.leftPos + 70 && mouseX <= this.leftPos + 92 && mouseY >= buttonY && mouseY < buttonY + 20) {
                    extractor.text(this.font, "§7☆", this.leftPos + 78, buttonY + 6, 0xFFAAAAAA, false);
                }
            }
        }
    }
}
