package com.easygame.generator

import com.easygame.mixin.BaseSpawnerAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.random.WeightedList
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.SpawnData
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.SpawnerBlockEntity
import java.util.Locale
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

object GeneratorStateManager {

    class GeneratorData(
        var storedCount: Int = 0,
        var progressTicks: Int = 0,
        var stackCount: Int = 1,
        var customItem: Item? = null,
        var customTier: BlockGenerator.RarityTier? = null,
        var lastItem: Item? = null
    )

    private val activeGenerators = ConcurrentHashMap<BlockPos, GeneratorData>()

    fun get(pos: BlockPos): GeneratorData? = activeGenerators[pos]

    fun getOrInit(pos: BlockPos): GeneratorData {
        return activeGenerators.computeIfAbsent(pos.immutable()) { GeneratorData() }
    }

    fun remove(pos: BlockPos): GeneratorData? = activeGenerators.remove(pos)

    fun updateSpawnerData(level: ServerLevel, pos: BlockPos, data: GeneratorData) {
        val be = level.getBlockEntity(pos)
        if (be is SpawnerBlockEntity) {
            val accessor = be.spawner as BaseSpawnerAccessor
            val entityTag = CompoundTag().apply {
                putString("id", "minecraft:item")
                if (data.customItem != null) {
                    putString("GeneratorItem", BuiltInRegistries.ITEM.getKey(data.customItem!!).toString())
                }
                putInt("GeneratorStack", data.stackCount)
            }
            val spawnData = SpawnData(entityTag, Optional.empty(), Optional.empty())
            accessor.setNextSpawnData(spawnData)
            accessor.setSpawnPotentials(WeightedList.of(spawnData))
            accessor.setRequiredPlayerRange(0)
            be.setChanged()
            val state = level.getBlockState(pos)
            level.sendBlockUpdated(pos, state, state, 3)
        }
    }

    fun tick(level: ServerLevel, pos: BlockPos) {
        val be = level.getBlockEntity(pos)
        val data = getOrInit(pos)

        // Read and sync NBT from SpawnerBlockEntity on first initialization
        if (be is SpawnerBlockEntity) {
            val accessor = be.spawner as BaseSpawnerAccessor
            val nextData = accessor.nextSpawnData
            val entityTag = nextData?.entityToSpawn()
            if (entityTag != null) {
                val itemIdOpt = entityTag.getString("GeneratorItem")
                if (itemIdOpt.isPresent) {
                    val id = Identifier.tryParse(itemIdOpt.get())
                    if (id != null) {
                        val item = BuiltInRegistries.ITEM.getValue(id)
                        if (item != Items.AIR && data.customItem == null) {
                            data.customItem = item
                            data.customTier = BlockGenerator.getItemTier(item)
                        }
                    }
                }
                val stackOpt = entityTag.getInt("GeneratorStack")
                if (stackOpt.isPresent && data.stackCount <= 1) {
                    data.stackCount = stackOpt.get().coerceAtLeast(1)
                }
            }
        }

        // Determine active item, tier, and status
        val item: Item
        val tier: BlockGenerator.RarityTier
        val isActive: Boolean

        if (data.customItem != null) {
            item = data.customItem!!
            tier = data.customTier ?: BlockGenerator.getItemTier(item)
            isActive = true
        } else {
            val result = BlockGenerator.checkSurroundings(level, pos)
            item = result.item
            tier = result.tier
            isActive = result.isActive
        }

        if (!isActive) {
            if (data.progressTicks > 0) {
                data.progressTicks = 0
            }
            return
        }

        data.lastItem = item

        // ACCELERATION: Stack count directly multiplies generation rate!
        data.progressTicks += data.stackCount
        val interval = tier.intervalTicks

        // Ambient visual effects
        val glowFreq = maxOf(1, 10 / data.stackCount.coerceAtMost(10))
        if (data.progressTicks % glowFreq == 0) {
            level.sendParticles(
                ParticleTypes.GLOW,
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                minOf(data.stackCount, 5), 0.35, 0.35, 0.35, 0.02
            )
        }

        // Surrounding energy glyphs (if surrounded by blocks)
        if (data.customItem == null && data.progressTicks % 10 == 0) {
            val randomOffset = BlockGenerator.NEIGHBOR_OFFSETS.random()
            val fromPos = pos.offset(randomOffset)
            level.sendParticles(
                ParticleTypes.ENCHANT,
                fromPos.x + 0.5, fromPos.y + 1.05, fromPos.z + 0.5,
                4,
                (pos.x - fromPos.x) * 0.25, 0.15, (pos.z - fromPos.z) * 0.25,
                0.25
            )
            level.sendParticles(
                ParticleTypes.WAX_ON,
                fromPos.x + 0.5, fromPos.y + 1.1, fromPos.z + 0.5,
                1, 0.1, 0.1, 0.1, 0.01
            )
        }

        // Generation cycle trigger
        if (data.progressTicks >= interval) {
            val cycles = data.progressTicks / interval
            data.progressTicks %= interval

            if (data.storedCount < 65536) {
                val newCount = (data.storedCount.toLong() + cycles).coerceAtMost(65536L).toInt()
                val added = newCount - data.storedCount
                data.storedCount = newCount

                if (added > 0) {
                    level.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        pos.x + 0.5, pos.y + 0.8, pos.z + 0.5,
                        minOf(8 + data.stackCount, 25), 0.35, 0.35, 0.35, 0.05
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
    }

    fun stackGenerator(player: ServerPlayer, level: ServerLevel, pos: BlockPos, heldStack: ItemStack): Boolean {
        val data = getOrInit(pos)

        // If sneaking, add entire hand stack; otherwise add 1
        val countToAdd = if (player.isShiftKeyDown) heldStack.count else 1
        if (countToAdd <= 0) return false

        // Check if held spawner is a configured generator item
        val heldItem = BlockGenerator.getGeneratorItem(heldStack)
        if (heldItem != null && data.customItem == null) {
            data.customItem = heldItem
            data.customTier = BlockGenerator.getItemTier(heldItem)
        }

        if (!player.hasInfiniteMaterials()) {
            heldStack.shrink(countToAdd)
        }

        data.stackCount += countToAdd
        updateSpawnerData(level, pos, data)

        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.5f)
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 1.0f, 1.2f)
        level.sendParticles(ParticleTypes.ENCHANT, pos.x + 0.5, pos.y + 1.2, pos.z + 0.5, 25, 0.4, 0.4, 0.4, 0.25)
        level.sendParticles(ParticleTypes.GLOW, pos.x + 0.5, pos.y + 0.6, pos.z + 0.5, 12, 0.3, 0.3, 0.3, 0.05)

        val activeItem = data.customItem ?: data.lastItem
        val tier = activeItem?.let { BlockGenerator.getItemTier(it) } ?: BlockGenerator.TIERS[1]!!
        val cycleSecs = String.format(Locale.US, "%.2f", tier.intervalSeconds / data.stackCount)

        player.sendSystemMessage(
            Component.literal("§6§l[Generator] §a⚡ STACKED +${countToAdd}x! §fTotal: §e${data.stackCount}x Generators §8| §bSpeed Multiplier: §e${data.stackCount}x §8(§b${cycleSecs}s/drop§8)!")
        )
        return true
    }

    fun configureItem(player: ServerPlayer, level: ServerLevel, pos: BlockPos, heldStack: ItemStack): Boolean {
        val targetItem = heldStack.item
        if (targetItem == Items.AIR) return false

        val data = getOrInit(pos)
        val tier = BlockGenerator.getItemTier(targetItem)
        data.customItem = targetItem
        data.customTier = tier
        data.progressTicks = 0

        updateSpawnerData(level, pos, data)

        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.4f)
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, 15, 0.4, 0.4, 0.4, 0.05)
        level.sendParticles(ParticleTypes.WAX_ON, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 8, 0.3, 0.3, 0.3, 0.02)

        val itemName = heldStack.hoverName.string
        val cycleSecs = String.format(Locale.US, "%.2f", tier.intervalSeconds / data.stackCount)

        player.sendSystemMessage(
            Component.literal("§6§l[Generator] §a✔ Configured to generate §e[${itemName}]§a! §7(Tier ${tier.tier} - ${tier.colorCode}${tier.name}§7, ${tier.intervalSeconds}s) §8| §fStack: §e${data.stackCount}x §8(§b${cycleSecs}s/drop§8)")
        )
        return true
    }

    fun handleRightClick(player: ServerPlayer, level: ServerLevel, pos: BlockPos, hand: InteractionHand): Boolean {
        val heldStack = player.getItemInHand(hand)

        // 1. Right click with a Spawner -> STACK generator in this place!
        if (heldStack.`is`(Items.SPAWNER)) {
            return stackGenerator(player, level, pos, heldStack)
        }

        // 2. Sneak + Right click with ANY item -> Configure this generator to produce that item!
        if (player.isShiftKeyDown && heldStack.item != Items.AIR) {
            return configureItem(player, level, pos, heldStack)
        }

        // 3. Normal Right click -> Inspect & Collect generated resources
        val data = getOrInit(pos)
        val activeItem = data.customItem ?: data.lastItem ?: run {
            val res = BlockGenerator.checkSurroundings(level, pos)
            if (res.isActive) res.item else null
        }

        if (activeItem != null) {
            val tier = BlockGenerator.getItemTier(activeItem)
            val stored = data.storedCount

            if (stored > 0) {
                // Collect all stored items directly into player inventory
                var remaining = stored
                while (remaining > 0) {
                    val batchSize = minOf(remaining, activeItem.defaultMaxStackSize)
                    val stack = ItemStack(activeItem, batchSize)
                    if (!player.inventory.add(stack)) {
                        val itemEntity = ItemEntity(level, player.x, player.y + 0.5, player.z, stack)
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

                val itemName = ItemStack(activeItem).hoverName.string
                player.sendSystemMessage(
                    Component.literal("§6§l[Generator] §a✔ Collected §e${stored}x ${itemName}§a!")
                )
                player.sendSystemMessage(
                    Component.literal("§7Tier ${tier.tier} (${tier.colorCode}${tier.name}§7) §8| §fStack: §e${data.stackCount}x §8| §fSpeed: §b${data.stackCount}x §8| §7Storage: §e0/65536")
                )
            } else {
                val interval = tier.intervalTicks
                val remainingTicks = maxOf(0, interval - data.progressTicks)
                val effectiveSecs = String.format(Locale.US, "%.1f", remainingTicks / (20.0 * data.stackCount))
                val cycleSecs = String.format(Locale.US, "%.2f", tier.intervalSeconds / data.stackCount)
                val itemName = ItemStack(activeItem).hoverName.string

                player.sendSystemMessage(
                    Component.literal("§6§l[Generator] §fResource: §e${itemName} §8| §a0 Stored §8| §7Tier ${tier.tier} (${tier.colorCode}${tier.name}§7) §8| §e${data.stackCount}x Stacked")
                )
                player.sendSystemMessage(
                    Component.literal("§7Status: §aActive §8| §7Next drop in: §e${effectiveSecs}s §8| §bSpeed: ${data.stackCount}x (§e${cycleSecs}s/drop§b)")
                )
            }
            return true
        } else {
            player.sendSystemMessage(
                Component.literal("§6§l[Generator] §cInactive: §7Shift-Right Click with ANY item to produce it standalone, OR place 8 matching blocks in a 3x3 around this spawner.")
            )
            val surroundingRes = BlockGenerator.checkSurroundings(level, pos)
            if (surroundingRes.matchingCount > 0) {
                val blockName = surroundingRes.block?.name?.string ?: "matching block"
                player.sendSystemMessage(
                    Component.literal("§7Current: §e${surroundingRes.matchingCount}/8 $blockName §8| §7Tier ${surroundingRes.tier.tier} (${surroundingRes.tier.colorCode}${surroundingRes.tier.name}§7, ${surroundingRes.tier.intervalSeconds}s)")
                )
            }
            return true
        }
    }

    fun handleBreak(level: ServerLevel, player: ServerPlayer, pos: BlockPos, hasSilkTouch: Boolean): Boolean {
        val data = remove(pos)

        // Read NBT if data wasn't in memory
        var stackCount = data?.stackCount ?: 1
        var configuredItem: Item? = data?.customItem ?: data?.lastItem
        var storedCount = data?.storedCount ?: 0

        val be = level.getBlockEntity(pos)
        if (be is SpawnerBlockEntity) {
            val accessor = be.spawner as BaseSpawnerAccessor
            val tag = accessor.nextSpawnData?.entityToSpawn()
            if (tag != null) {
                val itemIdOpt = tag.getString("GeneratorItem")
                if (itemIdOpt.isPresent && configuredItem == null) {
                    val id = Identifier.tryParse(itemIdOpt.get())
                    if (id != null) {
                        val item = BuiltInRegistries.ITEM.getValue(id)
                        if (item != Items.AIR) configuredItem = item
                    }
                }
                val stackOpt = tag.getInt("GeneratorStack")
                if (stackOpt.isPresent && stackCount <= 1) {
                    stackCount = stackOpt.get().coerceAtLeast(1)
                }
            }
        }

        if (configuredItem == null) {
            val res = BlockGenerator.checkSurroundings(level, pos)
            if (res.isActive) {
                configuredItem = res.item
            }
        }

        // 1. Recover all stored items directly into player inventory
        if (storedCount > 0 && configuredItem != null) {
            var remStored = storedCount
            while (remStored > 0) {
                val batchSize = minOf(remStored, configuredItem.defaultMaxStackSize)
                val stack = ItemStack(configuredItem, batchSize)
                if (!player.inventory.add(stack)) {
                    val entity = ItemEntity(level, player.x, player.y + 0.5, player.z, stack)
                    level.addFreshEntity(entity)
                }
                remStored -= batchSize
            }
        }

        // 2. Silk Touch: Break all and give ALL generators back in inventory as stackable items!
        if (hasSilkTouch) {
            var remGens = stackCount
            while (remGens > 0) {
                val batchSize = minOf(remGens, 64)
                val genStack = BlockGenerator.createGeneratorItemStack(configuredItem, batchSize, 1)
                if (!player.inventory.add(genStack)) {
                    val entity = ItemEntity(level, player.x, player.y + 0.5, player.z, genStack)
                    level.addFreshEntity(entity)
                }
                remGens -= batchSize
            }

            level.destroyBlock(pos, false)
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f)
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8f, 1.2f)
            level.sendParticles(ParticleTypes.WAX_ON, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 15, 0.3, 0.3, 0.3, 0.05)

            val itemName = configuredItem?.let { ItemStack(it).hoverName.string } ?: "Vanilla"
            player.sendSystemMessage(
                Component.literal("§6§l[Generator] §a✔ Silk Touch retrieved §e${stackCount}x §b${itemName} Generator §aand §e${storedCount}x ${itemName} §adirectly into your inventory!")
            )
            return false // Cancel vanilla break so no duplicate drops or XP
        } else {
            if (stackCount > 0) {
                player.sendSystemMessage(
                    Component.literal("§c⚠ Generator broken without Silk Touch! Stored items dropped, but Spawner was lost. Use Silk Touch next time to recover all ${stackCount}x generators!")
                )
            }
            return true // Allow vanilla break
        }
    }

    fun onSpawnerBroken(level: ServerLevel, pos: BlockPos) {
        val data = remove(pos)
        if (data != null && data.storedCount > 0 && data.lastItem != null) {
            var remaining = data.storedCount
            val item = data.lastItem!!
            while (remaining > 0) {
                val batchSize = minOf(remaining, item.defaultMaxStackSize)
                val stack = ItemStack(item, batchSize)
                val itemEntity = ItemEntity(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, stack)
                level.addFreshEntity(itemEntity)
                remaining -= batchSize
            }
        }
    }
}
