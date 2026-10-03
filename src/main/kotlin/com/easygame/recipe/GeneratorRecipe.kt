package com.easygame.recipe

import com.easygame.generator.BlockGenerator
import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class GeneratorRecipe : CustomRecipe() {

    override fun matches(input: CraftingInput, level: Level): Boolean {
        if (input.width() != 3 || input.height() != 3) {
            return false
        }

        // Center slot (1, 1) in 3x3 must be a Spawner
        val center = input.getItem(1, 1)
        if (!center.`is`(Items.SPAWNER)) {
            return false
        }

        // Check the 8 surrounding slots: all must be non-empty and have the exact same item
        var targetItem: Item? = null
        for (y in 0 until 3) {
            for (x in 0 until 3) {
                if (x == 1 && y == 1) continue // Skip center spawner
                val slot = input.getItem(x, y)
                if (slot.isEmpty) {
                    return false
                }
                if (targetItem == null) {
                    targetItem = slot.item
                } else if (slot.item != targetItem) {
                    return false
                }
            }
        }

        return targetItem != null && targetItem != Items.AIR
    }

    override fun assemble(input: CraftingInput): ItemStack {
        val targetItem = input.getItem(0, 0).item
        if (targetItem == Items.AIR) return ItemStack.EMPTY
        return BlockGenerator.createGeneratorItemStack(targetItem, 1, 1)
    }

    override fun getSerializer(): RecipeSerializer<out CustomRecipe> {
        return SERIALIZER
    }

    companion object {
        val INSTANCE = GeneratorRecipe()
        val MAP_CODEC: MapCodec<GeneratorRecipe> = MapCodec.unit(INSTANCE)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, GeneratorRecipe> = StreamCodec.unit(INSTANCE)
        val SERIALIZER: RecipeSerializer<GeneratorRecipe> = RecipeSerializer(MAP_CODEC, STREAM_CODEC)
    }
}
