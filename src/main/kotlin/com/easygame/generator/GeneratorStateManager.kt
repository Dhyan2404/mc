package com.easygame.generator

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object GeneratorStateManager {

    class GeneratorData(
        var storedCount: Int = 0,
        var progressTicks: Int = 0,
        var lastItem: Item? = null
    )

    private val activeGenerators = ConcurrentHashMap<BlockPos, GeneratorData>()

    fun getOrInit(pos: BlockPos): GeneratorData {
        return activeGenerators.computeIfAbsent(pos.immutable()) { GeneratorData() }
    }

    fun isGeneratorActive(level: ServerLevel, pos: BlockPos): Boolean {
        return BlockGenerator.checkSurroundings(level, pos).isActive
    }

    fun tick(level: ServerLevel, pos: BlockPos) {
        val result = BlockGenerator.checkSurroundings(level, pos)
        if (!result.isActive) {
            val data = activeGenerators[pos]
            if (data != null && data.progressTicks > 0) {
                data.progressTicks = 0
            }
            return
        }

        val data = getOrInit(pos)
        data.lastItem = result.item
        data.progressTicks++

        val interval = result.tier.intervalTicks

        // Ambient particles from surrounding blocks towards center spawner
        if (data.progressTicks % 20 == 0) {
            val randomOffset = BlockGenerator.NEIGHBOR_OFFSETS.random()
            val fromPos = pos.offset(randomOffset)
            level.sendParticles(
                ParticleTypes.ENCHANT,
                fromPos.x + 0.5, fromPos.y + 1.1, fromPos.z + 0.5,
                3,
                (pos.x - fromPos.x) * 0.2, 0.1, (pos.z - fromPos.z) * 0.2,
                0.2
            )
        }

        // Generation trigger
        if (data.progressTicks >= interval) {
            data.progressTicks = 0

            if (data.storedCount < 2048) {
                data.storedCount++

                // Generation visual effects
                level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.x + 0.5, pos.y + 0.8, pos.z + 0.5,
                    8, 0.35, 0.35, 0.35, 0.05
                )
                level.sendParticles(
                    ParticleTypes.WAX_ON,
                    pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                    4, 0.2, 0.2, 0.2, 0.02
                )
                level.playSound(
                    null, pos,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 0.8f, 1.4f
                )
            }
        }
    }

    fun handleRightClick(player: ServerPlayer, level: ServerLevel, pos: BlockPos): Boolean {
        val result = BlockGenerator.checkSurroundings(level, pos)
        val data = getOrInit(pos)

        if (result.isActive) {
            val item = data.lastItem ?: result.item
            val stored = data.storedCount

            if (stored > 0) {
                // Collect all items
                var remaining = stored
                while (remaining > 0) {
                    val batchSize = minOf(remaining, item.defaultMaxStackSize)
                    val stack = ItemStack(item, batchSize)
                    if (!player.inventory.add(stack)) {
                        // Drop in world if inventory full
                        val itemEntity = ItemEntity(
                            level,
                            player.x, player.y + 0.5, player.z,
                            stack
                        )
                        level.addFreshEntity(itemEntity)
                    }
                    remaining -= batchSize
                }

                data.storedCount = 0

                level.playSound(
                    null, player.blockPosition(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 1.0f, 1.2f
                )

                player.sendSystemMessage(
                    Component.literal("§6§l[Generator] §a✔ Collected §e${stored}x ${item.description.string}§a!")
                )
                player.sendSystemMessage(
                    Component.literal("§7Tier ${result.tier.tier} (${result.tier.colorCode}${result.tier.name}§7) §8| §fSpeed: §b1 every ${result.tier.intervalSeconds}s §8| §7Storage: §e0/2048")
                )
            } else {
                val remainingTicks = maxOf(0, result.tier.intervalTicks - data.progressTicks)
                val remainingSecs = String.format(Locale.US, "%.1f", remainingTicks / 20.0)

                player.sendSystemMessage(
                    Component.literal("§6§l[Generator] §fResource: §e${item.description.string} §8| §a0 Stored §8| §7Tier ${result.tier.tier} (${result.tier.colorCode}${result.tier.name}§7)")
                )
                player.sendSystemMessage(
                    Component.literal("§7Status: §aActive §8| §7Next drop in: §e${remainingSecs}s §8| §b1 every ${result.tier.intervalSeconds}s")
                )
            }
            return true
        } else {
            val blockName = result.block?.name?.string ?: "matching resource"
            player.sendSystemMessage(
                Component.literal("§6§l[Generator] §cInactive: §7Place 8 matching resource blocks in a 3x3 around this spawner to generate items.")
            )
            player.sendSystemMessage(
                Component.literal("§7Current: §e${result.matchingCount}/8 $blockName §8| §715 Rarity Tiers supported (Coal to Nether Star)!")
            )
            return true
        }
    }

    fun onSpawnerBroken(level: ServerLevel, pos: BlockPos) {
        val data = activeGenerators.remove(pos)
        if (data != null && data.storedCount > 0 && data.lastItem != null) {
            var remaining = data.storedCount
            val item = data.lastItem!!
            while (remaining > 0) {
                val batchSize = minOf(remaining, item.defaultMaxStackSize)
                val stack = ItemStack(item, batchSize)
                val itemEntity = ItemEntity(
                    level,
                    pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                    stack
                )
                level.addFreshEntity(itemEntity)
                remaining -= batchSize
            }
        }
    }
}
