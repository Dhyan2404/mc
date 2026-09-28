package com.easygame.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VaultServerData.class)
public class VaultServerDataMixin {

    /**
     * Requirement: Ominous Vaults have no limit of spawning items; player can insert infinite keys in a single vault.
     */
    @Inject(method = "hasRewardedPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHasRewardedPlayer(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
