package com.easygame.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a");

    // Lag-free cached fields (prevents per-frame string allocations)
    @Unique
    private static long lastTimeUpdateMs = 0;
    @Unique
    private static String cachedRealTime = "";

    @Unique
    private static int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    @Unique
    private static Direction lastDir = null;
    @Unique
    private static String cachedCoordText = "";

    @Unique
    private static long lastDayTime = -1;
    @Unique
    private static String cachedGameTime = "";

    @Unique
    private static int lastFps = -1;
    @Unique
    private static String cachedFpsText = "§f⚡ FPS: §a0";
    @Unique
    private static int cachedFpsColor = 0xFF00E676;
    @Unique
    private static int cachedFpsWidth = 0;

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
        long nowMs = System.currentTimeMillis();

        // 1. UPDATE TIME CACHE (Only once per second to prevent GC stutter)
        if (nowMs - lastTimeUpdateMs >= 1000) {
            lastTimeUpdateMs = nowMs;
            cachedRealTime = LocalTime.now().format(TIME_FORMATTER);
        }

        // 2. FPS SHOWER (Top-Right of Main Screen with sleek dark glass badge)
        int fps = this.minecraft.getFps();
        if (fps != lastFps || cachedFpsWidth == 0) {
            lastFps = fps;
            String fpsColor = fps >= 60 ? "§a" : (fps >= 30 ? "§e" : "§c");
            cachedFpsText = "§f⚡ FPS: " + fpsColor + fps;
            cachedFpsColor = fps >= 60 ? 0xFF00E676 : (fps >= 30 ? 0xFFFFD600 : 0xFFFF1744);
            cachedFpsWidth = font.width(cachedFpsText);
        }

        int fpsBadgePadding = 4;
        int fpsX = screenWidth - cachedFpsWidth - 8;
        int fpsY = 6;

        // Render sleek glass badge for FPS
        extractor.fill(fpsX - fpsBadgePadding - 2, fpsY - 3, screenWidth - 4, fpsY + font.lineHeight + 3, 0x77000000);
        // Colored indicator strip on left of FPS badge
        extractor.fill(fpsX - fpsBadgePadding - 2, fpsY - 3, fpsX - fpsBadgePadding, fpsY + font.lineHeight + 3, cachedFpsColor);
        // Text in crisp Minecraft font with drop shadow
        extractor.text(font, cachedFpsText, fpsX, fpsY, 0xFFFFFFFF, true);

        // 3. CURRENT TIME & COORDINATES (Top-Left of Main Screen with cohesive HUD card)
        LocalPlayer player = this.minecraft.player;
        int cardLeft = 6;
        int cardTop = 6;
        int cardPadding = 5;

        if (player != null) {
            // Update Coordinates Cache only when player moves to a different block
            int px = (int) Math.floor(player.getX());
            int py = (int) Math.floor(player.getY());
            int pz = (int) Math.floor(player.getZ());
            Direction dir = player.getDirection();

            if (px != lastX || py != lastY || pz != lastZ || dir != lastDir) {
                lastX = px;
                lastY = py;
                lastZ = pz;
                lastDir = dir;
                String dirName = dir != null ? dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1) : "Unknown";
                cachedCoordText = "§b📍 §fXYZ: §e" + px + ", " + py + ", " + pz + " §7(§f" + dirName + "§7)";
            }

            // Update Game Time Cache only when tick advances
            long dayTime = player.level().getOverworldClockTime();
            if (dayTime != lastDayTime) {
                lastDayTime = dayTime;
                long hours = (dayTime / 1000 + 6) % 24;
                long minutes = (dayTime % 1000) * 60 / 1000;
                boolean isDay = (hours >= 6 && hours < 18);
                cachedGameTime = String.format("%s §f%02d:%02d", isDay ? "§6☀" : "§9🌙", hours, minutes);
            }

            String fullTimeText = "§e⏰ §fTime: §a" + cachedRealTime + " §7| " + cachedGameTime;

            int coordWidth = font.width(cachedCoordText);
            int timeWidth = font.width(fullTimeText);
            int maxCardWidth = Math.max(coordWidth, timeWidth);
            int cardBottom = cardTop + (font.lineHeight * 2) + 5;

            // Translucent glass HUD background
            extractor.fill(cardLeft - 2, cardTop - 3, cardLeft + maxCardWidth + cardPadding + 2, cardBottom, 0x77000000);
            // Cyan accent strip on left
            extractor.fill(cardLeft - 2, cardTop - 3, cardLeft, cardBottom, 0xFF00E5FF);

            // Render Coordinates row
            extractor.text(font, cachedCoordText, cardLeft + 3, cardTop, 0xFFFFFFFF, true);
            // Render Time row
            extractor.text(font, fullTimeText, cardLeft + 3, cardTop + font.lineHeight + 2, 0xFFFFFFFF, true);
        } else {
            String fullTimeText = "§e⏰ §fTime: §a" + cachedRealTime;
            int timeWidth = font.width(fullTimeText);
            int cardBottom = cardTop + font.lineHeight + 3;

            extractor.fill(cardLeft - 2, cardTop - 3, cardLeft + timeWidth + cardPadding + 2, cardBottom, 0x77000000);
            extractor.fill(cardLeft - 2, cardTop - 3, cardLeft, cardBottom, 0xFF00E5FF);
            extractor.text(font, fullTimeText, cardLeft + 3, cardTop, 0xFFFFFFFF, true);
        }
    }
}
