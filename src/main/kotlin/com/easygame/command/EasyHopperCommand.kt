package com.easygame.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items

object EasyHopperCommand {

    private val EASY_HOPPER_NAME: Component = Component.literal("§4Easy Hopper")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val root = Commands.literal("easyhopper")
            .requires { true } // Allow in survival!
            .executes { ctx ->
                sendHelp(ctx.source)
                1
            }
            .then(
                Commands.literal("set")
                    .requires { true }
                    .executes { ctx ->
                        applyToHeld(ctx.source)
                    }
                    .then(
                        Commands.literal("inv")
                            .requires { true }
                            .executes { ctx ->
                                applyToInventory(ctx.source)
                            }
                    )
            )
            .then(
                Commands.literal("inv")
                    .requires { true }
                    .executes { ctx ->
                        applyToInventory(ctx.source)
                    }
            )
            .then(
                Commands.literal("clear")
                    .requires { true }
                    .executes { ctx ->
                        clearHeld(ctx.source)
                    }
                    .then(
                        Commands.literal("inv")
                            .requires { true }
                            .executes { ctx ->
                                clearInventory(ctx.source)
                            }
                    )
            )

        dispatcher.register(root)
    }

    private fun applyToHeld(source: CommandSourceStack): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("§cOnly players can run this command!"))
            return 0
        }

        val mainHand = player.mainHandItem
        val offHand = player.offhandItem

        val held = when {
            mainHand.`is`(Items.HOPPER) -> mainHand
            offHand.`is`(Items.HOPPER) -> offHand
            else -> null
        }

        if (held == null) {
            source.sendFailure(Component.literal("§c[EasyHopper] You must be holding a Hopper in your hand! (Use '/easyhopper set inv' to convert all hoppers in your inventory)"))
            return 0
        }

        held.set(DataComponents.CUSTOM_NAME, EASY_HOPPER_NAME)
        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS,
            1.0f,
            1.2f
        )
        source.sendSuccess(
            { Component.literal("§4[Easy Hopper] §aApplied to held hopper! Name set to §4Easy Hopper §a(64 items/tick).") },
            false
        )
        return 1
    }

    private fun applyToInventory(source: CommandSourceStack): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("§cOnly players can run this command!"))
            return 0
        }

        var totalHoppers = 0
        var totalStacks = 0

        val inv = player.inventory
        for (i in 0 until inv.containerSize) {
            val stack = inv.getItem(i)
            if (stack.`is`(Items.HOPPER)) {
                stack.set(DataComponents.CUSTOM_NAME, EASY_HOPPER_NAME)
                totalHoppers += stack.count
                totalStacks++
            }
        }

        if (totalStacks == 0) {
            source.sendFailure(Component.literal("§c[EasyHopper] No hoppers found in your inventory!"))
            return 0
        }

        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.PLAYER_LEVELUP,
            SoundSource.PLAYERS,
            1.0f,
            1.2f
        )
        source.sendSuccess(
            { Component.literal("§4[Easy Hopper] §aConverted all §e$totalHoppers §ahopper(s) ($totalStacks stack(s)) in your inventory to §4Easy Hopper §a(64 items/tick)!") },
            false
        )
        return 1
    }

    private fun clearHeld(source: CommandSourceStack): Int {
        val player = source.player ?: return 0
        val mainHand = player.mainHandItem
        val offHand = player.offhandItem

        val held = when {
            mainHand.`is`(Items.HOPPER) -> mainHand
            offHand.`is`(Items.HOPPER) -> offHand
            else -> null
        }

        if (held != null) {
            held.remove(DataComponents.CUSTOM_NAME)
            source.sendSuccess({ Component.literal("§a[EasyHopper] Held hopper reset to normal.") }, false)
            return 1
        }

        source.sendFailure(Component.literal("§c[EasyHopper] You must be holding a hopper to reset it."))
        return 0
    }

    private fun clearInventory(source: CommandSourceStack): Int {
        val player = source.player ?: return 0
        var count = 0
        val inv = player.inventory
        for (i in 0 until inv.containerSize) {
            val stack = inv.getItem(i)
            if (stack.`is`(Items.HOPPER)) {
                stack.remove(DataComponents.CUSTOM_NAME)
                count++
            }
        }
        source.sendSuccess({ Component.literal("§a[EasyHopper] Reset $count hopper stack(s) in inventory.") }, false)
        return 1
    }

    private fun sendHelp(source: CommandSourceStack) {
        source.sendSuccess(
            {
                Component.literal(
                    "§4=== Easy Hopper Commands ===\n" +
                    "§e/easyhopper set §7- Apply to held hopper only (§4Easy Hopper§7)\n" +
                    "§e/easyhopper set inv §7- Apply to ALL hoppers in your inventory\n" +
                    "§e/easyhopper clear §7- Reset held hopper\n" +
                    "§e/easyhopper clear inv §7- Reset all hoppers in inventory"
                )
            },
            false
        )
    }
}
