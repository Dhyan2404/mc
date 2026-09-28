package com.easygame.client.mixin;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    @Shadow
    protected abstract void openWorldLoadBundledResourcePack(LevelStorageSource.LevelStorageAccess access, WorldStem stem, PackRepository packRepo, Runnable onCancel);

    @Shadow
    protected abstract void upgradeAndOpenWorld(LevelStorageSource.LevelStorageAccess access, Dynamic<?> dynamic, Runnable onCancel);

    /**
     * Requirement: Never show warning screen when creating worlds (e.g. experimental or deprecated settings).
     */
    @Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onConfirmWorldCreation(Minecraft mc, CreateWorldScreen screen, Lifecycle lifecycle, Runnable onConfirm, boolean force, CallbackInfo ci) {
        onConfirm.run();
        ci.cancel();
    }

    /**
     * Requirement: Never show backup / unstable / experimental world warning when opening worlds.
     */
    @Inject(method = "openWorldCheckWorldStemCompatibility", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCheckStem(LevelStorageSource.LevelStorageAccess access, WorldStem stem, PackRepository packRepo, Runnable onCancel, CallbackInfo ci) {
        this.openWorldLoadBundledResourcePack(access, stem, packRepo, onCancel);
        ci.cancel();
    }

    /**
     * Requirement: Never show backup / unstable / experimental world warning when opening worlds.
     */
    @Inject(method = "askForBackup", at = @At("HEAD"), cancellable = true, require = 0)
    private void onAskForBackup(LevelStorageSource.LevelStorageAccess access, boolean customized, Runnable onCancel, Runnable onProceed, CallbackInfo ci) {
        onProceed.run();
        ci.cancel();
    }

    /**
     * Requirement: Never show version / downgrade / backup warning screen when playing worlds.
     */
    @Inject(method = "openWorldCheckVersionCompatibility", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCheckVersion(LevelStorageSource.LevelStorageAccess access, LevelSummary summary, Dynamic<?> dynamic, Runnable onCancel, CallbackInfo ci) {
        if (summary.isCompatible()) {
            this.upgradeAndOpenWorld(access, dynamic, onCancel);
            ci.cancel();
        }
    }
}
