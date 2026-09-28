package com.easygame.client.mixin;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConfirmExperimentalFeaturesScreen.class)
public abstract class ConfirmExperimentalFeaturesScreenMixin {

    @Shadow
    @Final
    private BooleanConsumer callback;

    /**
     * Automatically accept and bypass the experimental features warning screen.
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 0)
    private void onInit(CallbackInfo ci) {
        this.callback.accept(true);
        ci.cancel();
    }
}
