package com.easygame.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.phys.HitResult

object EasyHopperCommand {

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
                        applyHopper(ctx.source, isInv = false)
                    }
                    .then(
                        Commands.literal("inv")
                            .requires { true }
                            .executes { ctx ->
                                applyHopper(ctx.source, isInv = true)
                            }
                    )
                    .then(
                        Commands.literal("all")
                            .requires { true }
                            .executes { ctx ->
                                applyHopper(ctx.source, isInv = true)
                            }
                    )
                    .then(
                        Commands.literal("64")
                            .requires { true }
                            .executes { ctx ->
                                applyHopper(ctx.source, isInv = false)
                            }
                    )
            )
            .then(
                Commands.literal("clear")
                    .requires { true }
                    .executes { ctx ->
                        clearHopper(ctx.source)
                    }
            )

        dispatcher.register(root)
    }

    private fun applyHopper(source: CommandSourceStack, isInv: Boolean): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("§cOnly players can configure hoppers!"))
            return 0
        }

        val title = if (isInv) "§b✦ Insta Hopper (Full Inventory/tick)" else "§6✦ Insta Hopper (64/tick)"

        // 1. Check held item in main hand or off hand
        val mainHand = player.mainHandItem
        val offHand = player.offhandItem

        val heldHopper = when {
            mainHand.`is`(Items.HOPPER) -> mainHand
            offHand.`is`(Items.HOPPER) -> offHand
            else -> null
        }

        if (heldHopper != null) {
            heldHopper.set(DataComponents.CUSTOM_NAME, Component.literal(title))
            player.level().playSound(
                null,
                player.blockPosition(),
                if (isInv) SoundEvents.PLAYER_LEVELUP else SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                1.0f,
                1.2f
            )
            source.sendSuccess(
                { Component.literal("§a[EasyHopper] §fHeld Hopper configured to $title§a! Place it anywhere.") },
                false
            )
            return 1
        }

        // 2. Check if player is looking at a placed hopper block
        val hit = raycastBlock(player, 5.0)
        if (hit.type == HitResult.Type.BLOCK) {
            val be = player.level().getBlockEntity(hit.blockPos)
            if (be is HopperBlockEntity) {
                be.customName = Component.literal(title)
                be.setChanged()
                player.level().playSound(
                    null,
                    hit.blockPos,
                    if (isInv) SoundEvents.PLAYER_LEVELUP else SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS,
                    1.0f,
                    1.2f
                )
                source.sendSuccess(
                    { Component.literal("§a[EasyHopper] §fTargeted Hopper set to $title§a!") },
                    false
                )
                return 1
            }
        }

        source.sendFailure(
            Component.literal("§c[EasyHopper] Please hold a Hopper in your hand or look at a placed Hopper to configure it!")
        )
        return 0
    }

    private fun clearHopper(source: CommandSourceStack): Int {
        val player = source.player ?: return 0
        val mainHand = player.mainHandItem
        val offHand = player.offhandItem

        val heldHopper = when {
            mainHand.`is`(Items.HOPPER) -> mainHand
            offHand.`is`(Items.HOPPER) -> offHand
            else -> null
        }

        if (heldHopper != null) {
            heldHopper.remove(DataComponents.CUSTOM_NAME)
            source.sendSuccess({ Component.literal("§a[EasyHopper] Held Hopper reset to normal.") }, false)
            return 1
        }

        val hit = raycastBlock(player, 5.0)
        if (hit.type == HitResult.Type.BLOCK) {
            val be = player.level().getBlockEntity(hit.blockPos)
            if (be is HopperBlockEntity) {
                be.customName = null
                be.setChanged()
                source.sendSuccess({ Component.literal("§a[EasyHopper] Targeted Hopper reset to normal.") }, false)
                return 1
            }
        }

        source.sendFailure(Component.literal("§c[EasyHopper] Please hold a Hopper or look at a placed Hopper to reset."))
        return 0
    }

    private fun sendHelp(source: CommandSourceStack) {
        source.sendSuccess(
            {
                Component.literal(
                    "§6=== EasyHopper Commands ===\n" +
                    "§e/easyhopper set §7- Configure held or targeted hopper to 64 items/tick\n" +
                    "§e/easyhopper set inv §7- Configure held or targeted hopper to Full Inventory/tick\n" +
                    "§e/easyhopper clear §7- Reset hopper to vanilla behavior"
                )
            },
            false
        )
    }

    private fun raycastBlock(player: ServerPlayer, maxDistance: Double): net.minecraft.world.phys.BlockHitResult {
        val eyePos = player.eyePosition
        val viewVec = player.getViewVector(1.0f)
        val endPos = eyePos.add(viewVec.x * maxDistance, viewVec.y * maxDistance, viewVec.z * maxDistance)
        return player.level().clip(
            ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
            )
        )
    }
}
