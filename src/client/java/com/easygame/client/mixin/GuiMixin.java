package com.easygame.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
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

    // Zero-allocation cached fields to guarantee lag-free 144+ FPS rendering
    @Unique
    private static long lastTimeUpdateMs = 0;
    @Unique
    private static String cachedRealTime = "";

    @Unique
    private static int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    @Unique
    private static Direction lastDir = null;
    @Unique
    private static String cachedPosText = "";

    @Unique
    private static long lastDayTime = -1;
    @Unique
    private static String cachedGameTime = "";

    @Unique
    private static int lastFps = -1;
    @Unique
    private static String cachedFpsText = "§a0 §7FPS";
    @Unique
    private static int cachedFpsColor = 0xFF00E676;
    @Unique
    private static int cachedFpsWidth = 0;

    @Unique
    private static String formatBiomeName(String raw) {
        String[] words = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                if (i > 0) sb.append(" ");
                sb.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    sb.append(words[i].substring(1));
                }
            }
        }
        return sb.toString();
    }

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

        // 1. REAL-WORLD TIME (1-second cached refresh: 0 GC impact)
        if (nowMs - lastTimeUpdateMs >= 1000) {
            lastTimeUpdateMs = nowMs;
            cachedRealTime = LocalTime.now().format(TIME_FORMATTER);
        }

        // 2. SMALL, COMPACT TOP-RIGHT FPS PILL BADGE
        int fps = this.minecraft.getFps();
        if (fps != lastFps || cachedFpsWidth == 0) {
            lastFps = fps;
            String fpsColor = fps >= 60 ? "§a" : (fps >= 30 ? "§e" : "§c");
            cachedFpsText = "§f⚡ " + fpsColor + fps + " §7FPS";
            cachedFpsColor = fps >= 60 ? 0xFF00E676 : (fps >= 30 ? 0xFFFFD600 : 0xFFFF1744);
            cachedFpsWidth = font.width(cachedFpsText);
        }

        int fpsPaddingH = 4;
        int fpsX = screenWidth - cachedFpsWidth - 6;
        int fpsY = 4;
        int fpsRight = screenWidth - 3;
        int fpsBottom = fpsY + font.lineHeight + 1;
        int fpsLeft = fpsX - fpsPaddingH;

        // Render compact glass badge for FPS
        extractor.fill(fpsLeft, fpsY - 1, fpsRight, fpsBottom, 0x770E141E);
        extractor.fill(fpsLeft, fpsY - 1, fpsLeft + 1, fpsBottom, cachedFpsColor);
        extractor.text(font, cachedFpsText, fpsX, fpsY, 0xFFFFFFFF, true);

        // 3. SMALL, COMPACT TOP-LEFT HUD CARD (Position, Biome, Time)
        LocalPlayer player = this.minecraft.player;
        int cardLeft = 4;
        int cardTop = 4;
        int cardPaddingH = 4;

        if (player != null) {
            // Update Coordinates and Biome Cache only when player crosses block boundary
            int px = (int) Math.floor(player.getX());
            int py = (int) Math.floor(player.getY());
            int pz = (int) Math.floor(player.getZ());
            Direction dir = player.getDirection();

            if (px != lastX || py != lastY || pz != lastZ || dir != lastDir) {
                lastX = px;
                lastY = py;
                lastZ = pz;
                lastDir = dir;

                String dirName = dir != null ? dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1) : "?";

                Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());
                String biomeName = biomeHolder.unwrapKey()
                        .map(k -> (String) formatBiomeName(k.identifier().getPath()))
                        .orElse("Unknown");

                cachedPosText = "§b📍 §e" + px + " " + py + " " + pz + " §8| §f" + dirName + " §8| §a" + biomeName;
            }

            // Update Game Time Cache only when tick advances
            long dayTime = player.level().getOverworldClockTime();
            if (dayTime != lastDayTime) {
                lastDayTime = dayTime;
                long hours = (dayTime / 1000 + 6) % 24;
                long minutes = (dayTime % 1000) * 60 / 1000;
                long dayNumber = (dayTime / 24000L) + 1;
                boolean isDay = (hours >= 6 && hours < 18);
                cachedGameTime = String.format("%s §f%02d:%02d §7(Day %d)", isDay ? "§6☀" : "§9🌙", hours, minutes, dayNumber);
            }

            String fullTimeText = "§e⏰ §f" + cachedRealTime + " §8| " + cachedGameTime;

            int posWidth = font.width(cachedPosText);
            int timeWidth = font.width(fullTimeText);
            int maxContentWidth = Math.max(posWidth, timeWidth);

            int cardRight = cardLeft + maxContentWidth + cardPaddingH + 3;
            int lineHeight = font.lineHeight + 1;
            int cardBottom = cardTop + (lineHeight * 2);

            // Small, compact translucent glass HUD background
            extractor.fill(cardLeft, cardTop - 1, cardRight, cardBottom, 0x770E141E);
            extractor.fill(cardLeft, cardTop - 1, cardLeft + 1, cardBottom, 0xFF00E5FF);

            // Render 2 neat rows
            int textX = cardLeft + 3;
            extractor.text(font, cachedPosText, textX, cardTop, 0xFFFFFFFF, true);
            extractor.text(font, fullTimeText, textX, cardTop + lineHeight, 0xFFFFFFFF, true);
        } else {
            String fullTimeText = "§e⏰ §f" + cachedRealTime;
            int timeWidth = font.width(fullTimeText);
            int cardRight = cardLeft + timeWidth + cardPaddingH + 3;
            int cardBottom = cardTop + font.lineHeight + 1;

            extractor.fill(cardLeft, cardTop - 1, cardRight, cardBottom, 0x770E141E);
            extractor.fill(cardLeft, cardTop - 1, cardLeft + 1, cardBottom, 0xFF00E5FF);
            extractor.text(font, fullTimeText, cardLeft + 3, cardTop, 0xFFFFFFFF, true);
        }
    }
}
