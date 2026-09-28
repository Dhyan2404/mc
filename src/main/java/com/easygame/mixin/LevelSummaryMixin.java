package com.easygame.mixin;

import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelSummary.class)
public abstract class LevelSummaryMixin {

    /**
     * Requirement: Never treat worlds as experimental or dangerous in world selection lists.
     */
    @Inject(method = "isExperimental", at = @At("HEAD"), cancellable = true, require = 0)
    private void onIsExperimental(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Requirement: Never ask for backups when playing worlds.
     */
    @Inject(method = "shouldBackup", at = @At("HEAD"), cancellable = true, require = 0)
    private void onShouldBackup(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Requirement: Never flag world as downgrade.
     */
    @Inject(method = "isDowngrade", at = @At("HEAD"), cancellable = true, require = 0)
    private void onIsDowngrade(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Requirement: Return BackupStatus.NONE so no warning badge or prompt appears.
     */
    @Inject(method = "backupStatus", at = @At("HEAD"), cancellable = true, require = 0)
    private void onBackupStatus(CallbackInfoReturnable<LevelSummary.BackupStatus> cir) {
        cir.setReturnValue(LevelSummary.BackupStatus.NONE);
    }

    /**
     * Requirement: Always mark world as compatible.
     */
    @Inject(method = "isCompatible", at = @At("HEAD"), cancellable = true, require = 0)
    private void onIsCompatible(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /**
     * Requirement: Never disable world entry.
     */
    @Inject(method = "isDisabled", at = @At("HEAD"), cancellable = true, require = 0)
    private void onIsDisabled(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
