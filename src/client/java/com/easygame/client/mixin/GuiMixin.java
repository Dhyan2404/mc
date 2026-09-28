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
    private static String cachedCoordText = "";
    @Unique
    private static String cachedBiomeText = "";

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

        // 2. MODERN TOP-RIGHT FPS BADGE
        int fps = this.minecraft.getFps();
        if (fps != lastFps || cachedFpsWidth == 0) {
            lastFps = fps;
            String fpsColor = fps >= 60 ? "§a" : (fps >= 30 ? "§e" : "§c");
            cachedFpsText = "§f⚡ FPS: " + fpsColor + fps;
            cachedFpsColor = fps >= 60 ? 0xFF00E676 : (fps >= 30 ? 0xFFFFD600 : 0xFFFF1744);
            cachedFpsWidth = font.width(cachedFpsText);
        }

        int fpsBadgePadding = 6;
        int fpsX = screenWidth - cachedFpsWidth - 10;
        int fpsY = 6;
        int fpsRight = screenWidth - 4;
        int fpsBottom = fpsY + font.lineHeight + 4;
        int fpsLeft = fpsX - fpsBadgePadding;

        // Render sleek glass badge for FPS
        extractor.fill(fpsLeft, fpsY - 2, fpsRight, fpsBottom, 0x880E141E);
        // Left accent indicator
        extractor.fill(fpsLeft, fpsY - 2, fpsLeft + 2, fpsBottom, cachedFpsColor);
        // Subtle specular highlight on top
        extractor.fill(fpsLeft, fpsY - 2, fpsRight, fpsY - 1, 0x33FFFFFF);
        // Subtle drop shadow on bottom
        extractor.fill(fpsLeft, fpsBottom - 1, fpsRight, fpsBottom, 0x33000000);
        // FPS text with crisp drop shadow
        extractor.text(font, cachedFpsText, fpsX, fpsY, 0xFFFFFFFF, true);

        // 3. TOP-LEFT MODERN HUD CARD (Coordinates, Biome, Time)
        LocalPlayer player = this.minecraft.player;
        int cardLeft = 6;
        int cardTop = 6;
        int cardPadding = 6;

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

                String dirDetail = switch (dir) {
                    case NORTH -> "North §8(§7-Z§8)";
                    case SOUTH -> "South §8(§7+Z§8)";
                    case WEST -> "West §8(§7-X§8)";
                    case EAST -> "East §8(§7+X§8)";
                    default -> dir != null ? dir.getName() : "Unknown";
                };
                cachedCoordText = "§b📍 §fXYZ: §e" + px + " §7/ §e" + py + " §7/ §e" + pz + "  §8[§b" + dirDetail + "§8]";

                Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());
                String biomeName = biomeHolder.unwrapKey()
                        .map(k -> (String) formatBiomeName(k.identifier().getPath()))
                        .orElse("Unknown");
                cachedBiomeText = "§a🌿 §fBiome: §a" + biomeName;
            }

            // Update Game Time Cache only when tick advances
            long dayTime = player.level().getOverworldClockTime();
            if (dayTime != lastDayTime) {
                lastDayTime = dayTime;
                long hours = (dayTime / 1000 + 6) % 24;
                long minutes = (dayTime % 1000) * 60 / 1000;
                long dayNumber = (dayTime / 24000L) + 1;
                boolean isDay = (hours >= 6 && hours < 18);
                cachedGameTime = String.format("%s §f%02d:%02d  §7(Day %d)", isDay ? "§6☀" : "§9🌙", hours, minutes, dayNumber);
            }

            String fullTimeText = "§e⏰ §fTime: §a" + cachedRealTime + "  §8|  " + cachedGameTime;

            int coordWidth = font.width(cachedCoordText);
            int biomeWidth = font.width(cachedBiomeText);
            int timeWidth = font.width(fullTimeText);
            int maxContentWidth = Math.max(coordWidth, Math.max(biomeWidth, timeWidth));

            int cardRight = cardLeft + maxContentWidth + cardPadding + 4;
            int lineHeight = font.lineHeight + 3;
            int cardBottom = cardTop + (lineHeight * 3) + 2;

            // Premium Translucent Glass HUD Background
            extractor.fill(cardLeft, cardTop - 2, cardRight, cardBottom, 0x880E141E);
            // Cyan accent strip on left
            extractor.fill(cardLeft, cardTop - 2, cardLeft + 2, cardBottom, 0xFF00E5FF);
            // Specular glass highlight on top
            extractor.fill(cardLeft, cardTop - 2, cardRight, cardTop - 1, 0x33FFFFFF);
            // Soft shadow on bottom
            extractor.fill(cardLeft, cardBottom - 1, cardRight, cardBottom, 0x33000000);

            // Render Rows with crisp drop shadow
            int textX = cardLeft + 5;
            extractor.text(font, cachedCoordText, textX, cardTop, 0xFFFFFFFF, true);
            extractor.text(font, cachedBiomeText, textX, cardTop + lineHeight, 0xFFFFFFFF, true);
            extractor.text(font, fullTimeText, textX, cardTop + (lineHeight * 2), 0xFFFFFFFF, true);
        } else {
            String fullTimeText = "§e⏰ §fTime: §a" + cachedRealTime;
            int timeWidth = font.width(fullTimeText);
            int cardRight = cardLeft + timeWidth + cardPadding + 4;
            int cardBottom = cardTop + font.lineHeight + 4;

            extractor.fill(cardLeft, cardTop - 2, cardRight, cardBottom, 0x880E141E);
            extractor.fill(cardLeft, cardTop - 2, cardLeft + 2, cardBottom, 0xFF00E5FF);
            extractor.fill(cardLeft, cardTop - 2, cardRight, cardTop - 1, 0x33FFFFFF);
            extractor.fill(cardLeft, cardBottom - 1, cardRight, cardBottom, 0x33000000);
            extractor.text(font, fullTimeText, cardLeft + 5, cardTop, 0xFFFFFFFF, true);
        }
    }
}
