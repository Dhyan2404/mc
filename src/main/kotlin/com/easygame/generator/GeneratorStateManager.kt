package com.easygame.generator

import com.easygame.mixin.BaseSpawnerAccessor
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
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
import net.minecraft.world.level.storage.LevelResource
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

object GeneratorStateManager {

    data class GeneratorKey(
        val dimension: String,
        val pos: BlockPos
    )

    class GeneratorData(
        var dimension: String = "minecraft:overworld",
        var pos: BlockPos = BlockPos.ZERO,
        var storedCount: Int = 0,
        var progressTicks: Int = 0,
        var stackCount: Int = 1,
        var customItem: Item? = null,
        var customTier: BlockGenerator.RarityTier? = null,
        var lastItem: Item? = null,
        var lastTickedGameTime: Long = -1L
    )

    private val activeGenerators = ConcurrentHashMap<GeneratorKey, GeneratorData>()
    private val GSON = GsonBuilder().setPrettyPrinting().create()

    private fun makeKey(level: ServerLevel, pos: BlockPos): GeneratorKey {
        return GeneratorKey(level.dimension().identifier().toString(), pos.immutable())
    }

    fun get(pos: BlockPos): GeneratorData? {
        val imm = pos.immutable()
        return activeGenerators.entries.firstOrNull { it.key.pos == imm }?.value
    }

    fun getOrInit(level: ServerLevel, pos: BlockPos): GeneratorData {
        val key = makeKey(level, pos)
        val data = activeGenerators.computeIfAbsent(key) {
            GeneratorData(
                dimension = key.dimension,
                pos = key.pos
            )
        }
        ensureChunkForced(level, pos)
        return data
    }

    fun remove(level: ServerLevel, pos: BlockPos): GeneratorData? {
        val key = makeKey(level, pos)
        val removed = activeGenerators.remove(key)
        releaseChunkForced(level, pos)
        return removed
    }

    fun ensureChunkForced(level: ServerLevel, pos: BlockPos) {
        try {
            val chunkX = pos.x shr 4
            val chunkZ = pos.z shr 4
            level.setChunkForced(chunkX, chunkZ, true)
        } catch (_: Exception) {}
    }

    fun releaseChunkForced(level: ServerLevel, pos: BlockPos) {
        try {
            val chunkX = pos.x shr 4
            val chunkZ = pos.z shr 4
            val dim = level.dimension().identifier().toString()
            val anyLeft = activeGenerators.keys.any {
                it.dimension == dim && (it.pos.x shr 4) == chunkX && (it.pos.z shr 4) == chunkZ && it.pos != pos
            }
            if (!anyLeft) {
                level.setChunkForced(chunkX, chunkZ, false)
            }
        } catch (_: Exception) {}
    }

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
        ensureChunkForced(level, pos)
    }

    /**
     * Catches up generated items for any ticks missed while chunk was unloaded or server was offline
     */
    fun catchUp(level: ServerLevel, data: GeneratorData) {
        val currentGameTime = level.gameTime
        if (data.lastTickedGameTime > 0L && currentGameTime > data.lastTickedGameTime + 1) {
            val elapsedTicks = currentGameTime - data.lastTickedGameTime
            val item = data.customItem ?: data.lastItem ?: return
            val tier = data.customTier ?: BlockGenerator.getItemTier(item)
            val interval = tier.intervalTicks

            val totalProgress = data.progressTicks.toLong() + (elapsedTicks * data.stackCount.toLong())
            val cycles = totalProgress / interval
            data.progressTicks = (totalProgress % interval).toInt()
            data.storedCount = (data.storedCount.toLong() + cycles).coerceAtMost(65536L).toInt()
        }
        data.lastTickedGameTime = currentGameTime
    }

    /**
     * Global Server Tick: Executes every tick for all registered generators,
     * EVEN WHEN THE CHUNK IS UNLOADED!
     */
    fun onServerTick(server: MinecraftServer) {
        val overworld = server.overworld()
        val currentGameTime = overworld.gameTime

        for ((key, data) in activeGenerators) {
            // Avoid double-ticking if already ticked this exact game tick
            if (data.lastTickedGameTime == currentGameTime) continue
            data.lastTickedGameTime = currentGameTime

            val item = data.customItem ?: data.lastItem ?: continue
            val tier = data.customTier ?: BlockGenerator.getItemTier(item)

            data.progressTicks += data.stackCount
            val interval = tier.intervalTicks
            if (data.progressTicks >= interval) {
                val cycles = data.progressTicks / interval
                data.progressTicks %= interval

                if (data.storedCount < 65536) {
                    val newCount = (data.storedCount.toLong() + cycles).coerceAtMost(65536L).toInt()
                    data.storedCount = newCount
                }
            }
        }
    }

    /**
     * Local Block Entity Tick: Executes when the chunk IS loaded to produce visual particles & sounds.
     */
    fun tick(level: ServerLevel, pos: BlockPos) {
        val be = level.getBlockEntity(pos)
        val data = getOrInit(level, pos)

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

        // Catch-up any time that elapsed while chunk was unloaded
        catchUp(level, data)

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
        data.lastTickedGameTime = level.gameTime

        // Advance progress
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
        val data = getOrInit(level, pos)

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

        val data = getOrInit(level, pos)
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

        // 3. Normal Right click -> Catch-up & Inspect & Collect generated resources
        val data = getOrInit(level, pos)
        catchUp(level, data)

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
        val data = remove(level, pos)

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
                    Component.literal("§c⚠ Generator broken without Silk Touch! Stored items dropped, but Spawners were destroyed. Use Silk Touch next time to recover all ${stackCount}x generators!")
                )
            }
            return true // Allow vanilla break
        }
    }

    fun onSpawnerBroken(level: ServerLevel, pos: BlockPos) {
        val data = remove(level, pos)
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

    /**
     * File persistence: Save all active generators so they persist across server restarts
     */
    fun saveGenerators(server: MinecraftServer) {
        try {
            val file = getSaveFile(server)
            val jsonArray = JsonArray()
            for ((key, data) in activeGenerators) {
                val obj = JsonObject()
                obj.addProperty("dim", key.dimension)
                obj.addProperty("x", key.pos.x)
                obj.addProperty("y", key.pos.y)
                obj.addProperty("z", key.pos.z)
                obj.addProperty("stack", data.stackCount)
                obj.addProperty("stored", data.storedCount)
                obj.addProperty("progress", data.progressTicks)
                obj.addProperty("lastGameTime", data.lastTickedGameTime)
                if (data.customItem != null) {
                    obj.addProperty("item", BuiltInRegistries.ITEM.getKey(data.customItem!!).toString())
                } else if (data.lastItem != null) {
                    obj.addProperty("item", BuiltInRegistries.ITEM.getKey(data.lastItem!!).toString())
                }
                jsonArray.add(obj)
            }
            file.parentFile?.mkdirs()
            file.writeText(GSON.toJson(jsonArray), StandardCharsets.UTF_8)
        } catch (_: Exception) {}
    }

    /**
     * File persistence: Load saved generators on server boot and restore chunk forced tickets
     */
    fun loadSavedGenerators(server: MinecraftServer) {
        try {
            val file = getSaveFile(server)
            if (!file.exists()) return

            val text = file.readText(StandardCharsets.UTF_8)
            val element = JsonParser.parseString(text)
            if (!element.isJsonArray) return

            val array = element.asJsonArray
            for (itemElement in array) {
                if (!itemElement.isJsonObject) continue
                val obj = itemElement.asJsonObject

                val dim = obj.get("dim")?.asString ?: "minecraft:overworld"
                val x = obj.get("x")?.asInt ?: 0
                val y = obj.get("y")?.asInt ?: 0
                val z = obj.get("z")?.asInt ?: 0
                val pos = BlockPos(x, y, z)

                val key = GeneratorKey(dim, pos)
                val data = GeneratorData(
                    dimension = dim,
                    pos = pos,
                    stackCount = obj.get("stack")?.asInt ?: 1,
                    storedCount = obj.get("stored")?.asInt ?: 0,
                    progressTicks = obj.get("progress")?.asInt ?: 0,
                    lastTickedGameTime = obj.get("lastGameTime")?.asLong ?: -1L
                )

                if (obj.has("item")) {
                    val itemIdStr = obj.get("item").asString
                    val id = Identifier.tryParse(itemIdStr)
                    if (id != null) {
                        val item = BuiltInRegistries.ITEM.getValue(id)
                        if (item != Items.AIR) {
                            data.customItem = item
                            data.customTier = BlockGenerator.getItemTier(item)
                        }
                    }
                }

                activeGenerators[key] = data

                // Find matching server level and ensure chunk is forced
                val targetLevel = server.allLevels.find { it.dimension().identifier().toString() == dim } ?: server.overworld()
                ensureChunkForced(targetLevel, pos)
                // Catch-up any time that elapsed while the server was offline
                catchUp(targetLevel, data)
            }
        } catch (_: Exception) {}
    }

    private fun getSaveFile(server: MinecraftServer): File {
        val rootDir = server.getWorldPath(LevelResource.ROOT).toFile()
        return File(rootDir, "easygame_generators.json")
    }
}
