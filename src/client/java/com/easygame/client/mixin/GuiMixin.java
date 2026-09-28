package com.easygame.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow
    private Minecraft minecraft;

    @Shadow
    public abstract Font getFont();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a");

    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void onExtractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.options.hideGui) {
            return;
        }

        // Hide when F3 debug overlay is active to avoid overlapping
        if (this.minecraft.getDebugOverlay() != null && this.minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }

        Font font = this.getFont();
        if (font == null) return;

        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();

        // 1. FPS shower on the right side of the main screen in Minecraft font
        int fps = this.minecraft.getFps();
        String fpsColor = fps >= 60 ? "§a" : (fps >= 30 ? "§e" : "§c");
        String fpsText = "§fFPS: " + fpsColor + fps;
        int fpsWidth = font.width(fpsText);
        int fpsX = screenWidth - fpsWidth - 6;
        int fpsY = 6;
        extractor.text(font, fpsText, fpsX, fpsY, 0xFFFFFFFF, true);

        // 2. Current time and coordinates on the top left in Minecraft font
        LocalPlayer player = this.minecraft.player;
        int leftX = 6;
        int lineY = 6;

        if (player != null) {
            // Coordinates (XYZ)
            int x = (int) Math.floor(player.getX());
            int y = (int) Math.floor(player.getY());
            int z = (int) Math.floor(player.getZ());
            String coordText = "§eXYZ: §f" + x + ", " + y + ", " + z;
            extractor.text(font, coordText, leftX, lineY, 0xFFFFFFFF, true);
            lineY += font.lineHeight + 2;

            // Current Time (Real-world 12hr time + In-game clock time)
            LocalTime now = LocalTime.now();
            String realTime = now.format(TIME_FORMATTER);
            long dayTime = player.level().getOverworldClockTime();
            long hours = (dayTime / 1000 + 6) % 24;
            long minutes = (dayTime % 1000) * 60 / 1000;
            String gameTime = String.format("%02d:%02d", hours, minutes);
            String timeText = "§eTime: §f" + realTime + " §7(§b" + gameTime + "§7)";
            extractor.text(font, timeText, leftX, lineY, 0xFFFFFFFF, true);
        } else {
            LocalTime now = LocalTime.now();
            String realTime = now.format(TIME_FORMATTER);
            String timeText = "§eTime: §f" + realTime;
            extractor.text(font, timeText, leftX, lineY, 0xFFFFFFFF, true);
        }
    }
}
