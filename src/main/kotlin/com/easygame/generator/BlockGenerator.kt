package com.easygame.generator

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object BlockGenerator {

    data class RarityTier(
        val tier: Int,
        val name: String,
        val colorCode: String,
        val intervalTicks: Int // 20 ticks = 1 second
    ) {
        val intervalSeconds: Double get() = intervalTicks / 20.0
    }

    // 15 distinct rarity tiers with progressive generation intervals
    val TIERS = mapOf(
        1 to RarityTier(1, "Common", "§7", 30),        // 1.5s
        2 to RarityTier(2, "Organic", "§a", 50),       // 2.5s
        3 to RarityTier(3, "Fuel & Earth", "§8", 70),  // 3.5s
        4 to RarityTier(4, "Copper & Glass", "§6", 90),// 4.5s
        5 to RarityTier(5, "Iron", "§f", 120),         // 6.0s
        6 to RarityTier(6, "Lapis", "§9", 160),        // 8.0s
        7 to RarityTier(7, "Redstone", "§c", 200),     // 10.0s
        8 to RarityTier(8, "Gold", "§e", 260),         // 13.0s
        9 to RarityTier(9, "Quartz & Glow", "§d", 340),// 17.0s
        10 to RarityTier(10, "Crystal & Prism", "§5", 440), // 22.0s
        11 to RarityTier(11, "Obsidian & Resin", "§1", 560), // 28.0s
        12 to RarityTier(12, "Emerald", "§2", 700),     // 35.0s
        13 to RarityTier(13, "Diamond", "§b", 900),     // 45.0s
        14 to RarityTier(14, "Netherite & End", "§4", 1200), // 60.0s
        15 to RarityTier(15, "Mythic & Relic", "§6§l", 1600)  // 80.0s
    )

    data class BlockMapping(
        val item: Item,
        val tier: Int
    )

    private val MAPPINGS = HashMap<Block, BlockMapping>().apply {
        // TIER 1: Common Earth & Stone (1.5s)
        put(Blocks.COBBLESTONE, BlockMapping(Items.COBBLESTONE, 1))
        put(Blocks.STONE, BlockMapping(Items.STONE, 1))
        put(Blocks.SMOOTH_STONE, BlockMapping(Items.SMOOTH_STONE, 1))
        put(Blocks.COBBLED_DEEPSLATE, BlockMapping(Items.COBBLED_DEEPSLATE, 1))
        put(Blocks.DEEPSLATE, BlockMapping(Items.DEEPSLATE, 1))
        put(Blocks.ANDESITE, BlockMapping(Items.ANDESITE, 1))
        put(Blocks.DIORITE, BlockMapping(Items.DIORITE, 1))
        put(Blocks.GRANITE, BlockMapping(Items.GRANITE, 1))
        put(Blocks.TUFF, BlockMapping(Items.TUFF, 1))
        put(Blocks.DIRT, BlockMapping(Items.DIRT, 1))
        put(Blocks.GRASS_BLOCK, BlockMapping(Items.DIRT, 1))
        put(Blocks.SAND, BlockMapping(Items.SAND, 1))
        put(Blocks.RED_SAND, BlockMapping(Items.RED_SAND, 1))
        put(Blocks.GRAVEL, BlockMapping(Items.GRAVEL, 1))
        put(Blocks.NETHERRACK, BlockMapping(Items.NETHERRACK, 1))

        // TIER 2: Organic & Foliage (2.5s)
        put(Blocks.OAK_LOG, BlockMapping(Items.OAK_LOG, 2))
        put(Blocks.BIRCH_LOG, BlockMapping(Items.BIRCH_LOG, 2))
        put(Blocks.SPRUCE_LOG, BlockMapping(Items.SPRUCE_LOG, 2))
        put(Blocks.JUNGLE_LOG, BlockMapping(Items.JUNGLE_LOG, 2))
        put(Blocks.ACACIA_LOG, BlockMapping(Items.ACACIA_LOG, 2))
        put(Blocks.DARK_OAK_LOG, BlockMapping(Items.DARK_OAK_LOG, 2))
        put(Blocks.MANGROVE_LOG, BlockMapping(Items.MANGROVE_LOG, 2))
        put(Blocks.CHERRY_LOG, BlockMapping(Items.CHERRY_LOG, 2))
        put(Blocks.PALE_OAK_LOG, BlockMapping(Items.PALE_OAK_LOG, 2))
        put(Blocks.CRIMSON_STEM, BlockMapping(Items.CRIMSON_STEM, 2))
        put(Blocks.WARPED_STEM, BlockMapping(Items.WARPED_STEM, 2))
        put(Blocks.BAMBOO_BLOCK, BlockMapping(Items.BAMBOO, 2))
        put(Blocks.PUMPKIN, BlockMapping(Items.PUMPKIN, 2))
        put(Blocks.MELON, BlockMapping(Items.MELON_SLICE, 2))
        put(Blocks.CACTUS, BlockMapping(Items.CACTUS, 2))
        put(Blocks.SUGAR_CANE, BlockMapping(Items.SUGAR_CANE, 2))
        put(Blocks.HAY_BLOCK, BlockMapping(Items.WHEAT, 2))

        // TIER 3: Fuel & Earth (3.5s)
        put(Blocks.COAL_BLOCK, BlockMapping(Items.COAL, 3))
        put(Blocks.COAL_ORE, BlockMapping(Items.COAL, 3))
        put(Blocks.DEEPSLATE_COAL_ORE, BlockMapping(Items.COAL, 3))
        put(Blocks.CLAY, BlockMapping(Items.CLAY_BALL, 3))
        put(Blocks.MUD, BlockMapping(Items.MUD, 3))
        put(Blocks.TERRACOTTA, BlockMapping(Items.TERRACOTTA, 3))
        put(Blocks.BASALT, BlockMapping(Items.BASALT, 3))
        put(Blocks.BLACKSTONE, BlockMapping(Items.BLACKSTONE, 3))

        // TIER 4: Copper & Glass (4.5s)
        Blocks.COPPER_BLOCK.asList().forEach { put(it, BlockMapping(Items.COPPER_INGOT, 4)) }
        put(Blocks.RAW_COPPER_BLOCK, BlockMapping(Items.RAW_COPPER, 4))
        put(Blocks.COPPER_ORE, BlockMapping(Items.COPPER_INGOT, 4))
        put(Blocks.DEEPSLATE_COPPER_ORE, BlockMapping(Items.COPPER_INGOT, 4))
        put(Blocks.GLASS, BlockMapping(Items.GLASS, 4))
        put(Blocks.TINTED_GLASS, BlockMapping(Items.TINTED_GLASS, 4))

        // TIER 5: Iron (6.0s)
        put(Blocks.IRON_BLOCK, BlockMapping(Items.IRON_INGOT, 5))
        put(Blocks.RAW_IRON_BLOCK, BlockMapping(Items.RAW_IRON, 5))
        put(Blocks.IRON_ORE, BlockMapping(Items.IRON_INGOT, 5))
        put(Blocks.DEEPSLATE_IRON_ORE, BlockMapping(Items.IRON_INGOT, 5))

        // TIER 6: Lapis (8.0s)
        put(Blocks.LAPIS_BLOCK, BlockMapping(Items.LAPIS_LAZULI, 6))
        put(Blocks.LAPIS_ORE, BlockMapping(Items.LAPIS_LAZULI, 6))
        put(Blocks.DEEPSLATE_LAPIS_ORE, BlockMapping(Items.LAPIS_LAZULI, 6))

        // TIER 7: Redstone (10.0s)
        put(Blocks.REDSTONE_BLOCK, BlockMapping(Items.REDSTONE, 7))
        put(Blocks.REDSTONE_ORE, BlockMapping(Items.REDSTONE, 7))
        put(Blocks.DEEPSLATE_REDSTONE_ORE, BlockMapping(Items.REDSTONE, 7))
        put(Blocks.TARGET, BlockMapping(Items.TARGET, 7))
        put(Blocks.OBSERVER, BlockMapping(Items.OBSERVER, 7))

        // TIER 8: Gold (13.0s)
        put(Blocks.GOLD_BLOCK, BlockMapping(Items.GOLD_INGOT, 8))
        put(Blocks.RAW_GOLD_BLOCK, BlockMapping(Items.RAW_GOLD, 8))
        put(Blocks.GOLD_ORE, BlockMapping(Items.GOLD_INGOT, 8))
        put(Blocks.DEEPSLATE_GOLD_ORE, BlockMapping(Items.GOLD_INGOT, 8))
        put(Blocks.NETHER_GOLD_ORE, BlockMapping(Items.GOLD_NUGGET, 8))

        // TIER 9: Quartz & Glow (17.0s)
        put(Blocks.QUARTZ_BLOCK, BlockMapping(Items.QUARTZ, 9))
        put(Blocks.NETHER_QUARTZ_ORE, BlockMapping(Items.QUARTZ, 9))
        put(Blocks.GLOWSTONE, BlockMapping(Items.GLOWSTONE_DUST, 9))
        put(Blocks.SHROOMLIGHT, BlockMapping(Items.SHROOMLIGHT, 9))
        put(Blocks.MAGMA_BLOCK, BlockMapping(Items.MAGMA_CREAM, 9))
        put(Blocks.SOUL_SAND, BlockMapping(Items.SOUL_SAND, 9))

        // TIER 10: Crystal & Prism (22.0s)
        put(Blocks.AMETHYST_BLOCK, BlockMapping(Items.AMETHYST_SHARD, 10))
        put(Blocks.BUDDING_AMETHYST, BlockMapping(Items.AMETHYST_SHARD, 10))
        put(Blocks.PRISMARINE, BlockMapping(Items.PRISMARINE_SHARD, 10))
        put(Blocks.PRISMARINE_BRICKS, BlockMapping(Items.PRISMARINE_SHARD, 10))
        put(Blocks.DARK_PRISMARINE, BlockMapping(Items.PRISMARINE_SHARD, 10))
        put(Blocks.SEA_LANTERN, BlockMapping(Items.PRISMARINE_CRYSTALS, 10))
        put(Blocks.SPONGE, BlockMapping(Items.SPONGE, 10))

        // TIER 11: Obsidian & Hardened (28.0s)
        put(Blocks.OBSIDIAN, BlockMapping(Items.OBSIDIAN, 11))
        put(Blocks.CRYING_OBSIDIAN, BlockMapping(Items.CRYING_OBSIDIAN, 11))
        put(Blocks.REINFORCED_DEEPSLATE, BlockMapping(Items.REINFORCED_DEEPSLATE, 11))
        put(Blocks.SCULK_CATALYST, BlockMapping(Items.SCULK_CATALYST, 11))
        put(Blocks.SCULK_SHRIEKER, BlockMapping(Items.SCULK_SHRIEKER, 11))

        // TIER 12: Emerald (35.0s)
        put(Blocks.EMERALD_BLOCK, BlockMapping(Items.EMERALD, 12))
        put(Blocks.EMERALD_ORE, BlockMapping(Items.EMERALD, 12))
        put(Blocks.DEEPSLATE_EMERALD_ORE, BlockMapping(Items.EMERALD, 12))

        // TIER 13: Diamond (45.0s)
        put(Blocks.DIAMOND_BLOCK, BlockMapping(Items.DIAMOND, 13))
        put(Blocks.DIAMOND_ORE, BlockMapping(Items.DIAMOND, 13))
        put(Blocks.DEEPSLATE_DIAMOND_ORE, BlockMapping(Items.DIAMOND, 13))

        // TIER 14: Netherite & End Realm (60.0s)
        put(Blocks.ANCIENT_DEBRIS, BlockMapping(Items.NETHERITE_SCRAP, 14))
        put(Blocks.NETHERITE_BLOCK, BlockMapping(Items.NETHERITE_INGOT, 14))
        put(Blocks.END_STONE, BlockMapping(Items.END_STONE, 14))
        put(Blocks.PURPUR_BLOCK, BlockMapping(Items.POPPED_CHORUS_FRUIT, 14))
        put(Blocks.ENDER_CHEST, BlockMapping(Items.ENDER_PEARL, 14))
        put(Blocks.SHULKER_BOX, BlockMapping(Items.SHULKER_SHELL, 14))

        // TIER 15: Mythic & Relic (80.0s)
        put(Blocks.BEACON, BlockMapping(Items.NETHER_STAR, 15))
        put(Blocks.DRAGON_EGG, BlockMapping(Items.DRAGON_BREATH, 15))
        put(Blocks.HEAVY_CORE, BlockMapping(Items.HEAVY_CORE, 15))
        put(Blocks.CONDUIT, BlockMapping(Items.CONDUIT, 15))
        put(Blocks.VAULT, BlockMapping(Items.OMINOUS_TRIAL_KEY, 15))
    }

    fun getMapping(block: Block): BlockMapping {
        MAPPINGS[block]?.let { return it }

        // Dynamic fallback mapping
        val item = block.asItem()
        if (item == Items.AIR) {
            return BlockMapping(Items.COBBLESTONE, 1)
        }
        val hardness = block.defaultDestroyTime()
        val tier = when {
            hardness >= 50.0f -> 14
            hardness >= 25.0f -> 11
            hardness >= 5.0f -> 8
            hardness >= 3.0f -> 5
            hardness >= 1.5f -> 3
            else -> 1
        }
        return BlockMapping(item, tier)
    }

    data class SurroundingsResult(
        val isActive: Boolean,
        val matchingCount: Int,
        val block: Block?,
        val item: Item,
        val tier: RarityTier
    )

    // The 8 horizontal neighbor offsets around the central spawner
    val NEIGHBOR_OFFSETS = arrayOf(
        BlockPos(-1, 0, -1),
        BlockPos(0, 0, -1),
        BlockPos(1, 0, -1),
        BlockPos(-1, 0, 0),
        BlockPos(1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, 1)
    )

    fun checkSurroundings(level: Level, spawnerPos: BlockPos): SurroundingsResult {
        val blockCounts = HashMap<Block, Int>()

        for (offset in NEIGHBOR_OFFSETS) {
            val checkPos = spawnerPos.offset(offset)
            val block = level.getBlockState(checkPos).block
            if (block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR) {
                blockCounts[block] = blockCounts.getOrDefault(block, 0) + 1
            }
        }

        if (blockCounts.isEmpty()) {
            val defaultTier = TIERS[1]!!
            return SurroundingsResult(false, 0, null, Items.COBBLESTONE, defaultTier)
        }

        // Find majority surrounding block
        val topEntry = blockCounts.entries.maxByOrNull { it.value }!!
        val primaryBlock = topEntry.key
        val count = topEntry.value

        val mapping = getMapping(primaryBlock)
        val tier = TIERS[mapping.tier] ?: TIERS[1]!!

        // Full activation requires all 8 surrounding blocks to match
        val isActive = (count == 8)

        return SurroundingsResult(isActive, count, primaryBlock, mapping.item, tier)
    }
}
