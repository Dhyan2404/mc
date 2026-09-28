package com.easygame.command;

import com.easygame.mixin.CommandNodeAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

public class TeleportSurvivalHelper {

    /**
     * Requirement: allow /tp and /teleport commands in Survival without OP.
     */
    public static void allowInSurvival(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> tp = dispatcher.getRoot().getChild("tp");
        if (tp != null) {
            applyRecursive(tp);
        }
        CommandNode<CommandSourceStack> teleport = dispatcher.getRoot().getChild("teleport");
        if (teleport != null) {
            applyRecursive(teleport);
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyRecursive(CommandNode<CommandSourceStack> node) {
        if (node instanceof CommandNodeAccessor accessor) {
            accessor.easygame$setRequirement(src -> true);
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            applyRecursive(child);
        }
    }
}
