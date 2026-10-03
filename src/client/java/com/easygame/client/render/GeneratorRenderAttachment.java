package com.easygame.client.render;

import net.minecraft.world.item.ItemStack;

public interface GeneratorRenderAttachment {
    ItemStack easygame$getGeneratingStack();
    void easygame$setGeneratingStack(ItemStack stack);
    int easygame$getGlowColor();
    void easygame$setGlowColor(int color);
    float easygame$getCustomSpin();
    void easygame$setCustomSpin(float spin);
}
