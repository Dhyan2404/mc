package com.easygame.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private static int lastTitleFps = -1;
    @Unique
    private static String cachedTitleFpsText = "§a0 §7FPS";
    @Unique
    private static int cachedTitleFpsColor = 0xFF00E676;
    @Unique
    private static int cachedTitleFpsWidth = 0;

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
        if (fps != lastTitleFps || cachedTitleFpsWidth == 0) {
            lastTitleFps = fps;
            String fpsColor = fps >= 60 ? "§a" : (fps >= 30 ? "§e" : "§c");
            cachedTitleFpsText = "§f⚡ " + fpsColor + fps + " §7FPS";
            cachedTitleFpsColor = fps >= 60 ? 0xFF00E676 : (fps >= 30 ? 0xFFFFD600 : 0xFFFF1744);
            cachedTitleFpsWidth = font.width(cachedTitleFpsText);
        }

        int fpsPaddingH = 4;
        int fpsX = this.width - cachedTitleFpsWidth - 6;
        int fpsY = 4;
        int fpsRight = this.width - 3;
        int fpsBottom = fpsY + font.lineHeight + 1;
        int fpsLeft = fpsX - fpsPaddingH;

        // Render compact glass badge for FPS
        extractor.fill(fpsLeft, fpsY - 1, fpsRight, fpsBottom, 0x770E141E);
        extractor.fill(fpsLeft, fpsY - 1, fpsLeft + 1, fpsBottom, cachedTitleFpsColor);
        extractor.text(font, cachedTitleFpsText, fpsX, fpsY, 0xFFFFFFFF, true);
    }
}
