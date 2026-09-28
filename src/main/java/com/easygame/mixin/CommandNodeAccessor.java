package com.easygame.mixin;

import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(value = CommandNode.class, remap = false)
public interface CommandNodeAccessor<S> {

    @Accessor("requirement")
    void easygame$setRequirement(Predicate<S> requirement);

    @Accessor("requirement")
    Predicate<S> easygame$getRequirement();
}
