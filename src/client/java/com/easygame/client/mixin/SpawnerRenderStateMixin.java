package com.easygame.client.mixin;

import com.easygame.client.render.GeneratorRenderAttachment;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SpawnerRenderState.class)
public class SpawnerRenderStateMixin implements GeneratorRenderAttachment {

    @Unique
    private ItemStack easygame$generatingStack;

    @Unique
    private int easygame$glowColor;

    @Unique
    private float easygame$customSpin;

    @Override
    public ItemStack easygame$getGeneratingStack() {
        return this.easygame$generatingStack;
    }

    @Override
    public void easygame$setGeneratingStack(ItemStack stack) {
        this.easygame$generatingStack = stack;
    }

    @Override
    public int easygame$getGlowColor() {
        return this.easygame$glowColor;
    }

    @Override
    public void easygame$setGlowColor(int color) {
        this.easygame$glowColor = color;
    }

    @Override
    public float easygame$getCustomSpin() {
        return this.easygame$customSpin;
    }

    @Override
    public void easygame$setCustomSpin(float spin) {
        this.easygame$customSpin = spin;
    }
}
