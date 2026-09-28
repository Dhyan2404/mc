package com.easygame.client.mixin;

import net.minecraft.client.gui.screens.BackupConfirmScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackupConfirmScreen.class)
public abstract class BackupConfirmScreenMixin {

    @Shadow
    @Final
    protected BackupConfirmScreen.Listener onProceed;

    /**
     * Automatically proceed without backup so the player is never blocked by a backup prompt.
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 0)
    private void onInit(CallbackInfo ci) {
        if (this.onProceed != null) {
            this.onProceed.proceed(false, false);
            ci.cancel();
        }
    }
}
