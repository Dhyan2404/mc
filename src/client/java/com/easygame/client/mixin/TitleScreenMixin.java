package com.easygame.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void onExtractTitleScreen(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Font font = mc.font;
        if (font == null) return;

        int fps = mc.getFps();
        String fpsColor = fps >= 60 ? "§a" : (fps >= 30 ? "§e" : "§c");
        String fpsText = "§fFPS: " + fpsColor + fps;
        int fpsWidth = font.width(fpsText);
        int fpsX = this.width - fpsWidth - 6;
        int fpsY = 6;
        extractor.text(font, fpsText, fpsX, fpsY, 0xFFFFFFFF, true);
    }
}
