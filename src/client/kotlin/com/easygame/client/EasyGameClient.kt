package com.easygame.client

import com.easygame.network.CycleTradesPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

object EasyGameClient : ClientModInitializer {
    val lockedTradeIndices = mutableSetOf<Int>()

    override fun onInitializeClient() {
        // Enchantment description tooltip callback
        ItemTooltipCallback.EVENT.register { stack: ItemStack, _, _: TooltipFlag, lines: MutableList<Component> ->
            val addedKeys = mutableSetOf<String>()

            // Check stored enchantments (Enchanted Books)
            stack.get(DataComponents.STORED_ENCHANTMENTS)?.keySet()?.forEach { holder ->
                holder.unwrapKey().ifPresent { key ->
                    val path = key.identifier().path
                    if (addedKeys.add(path)) {
                        val desc = EnchantmentDescriptions.getDescription(path)
                        if (desc != null) {
                            val formattedName = path.replace('_', ' ').split(" ").joinToString(" ") { word ->
                                word.replaceFirstChar { it.uppercase() }
                            }
                            lines.add(Component.literal("§6✦ §e$formattedName: §7$desc"))
                        }
                    }
                }
            }

            // Check item enchantments (Swords, Bows, Armor, Tools)
            stack.get(DataComponents.ENCHANTMENTS)?.keySet()?.forEach { holder ->
                holder.unwrapKey().ifPresent { key ->
                    val path = key.identifier().path
                    if (addedKeys.add(path)) {
                        val desc = EnchantmentDescriptions.getDescription(path)
                        if (desc != null) {
                            val formattedName = path.replace('_', ' ').split(" ").joinToString(" ") { word ->
                                word.replaceFirstChar { it.uppercase() }
                            }
                            lines.add(Component.literal("§6✦ §e$formattedName: §7$desc"))
                        }
                    }
                }
            }
        }
    }

    fun isLocked(index: Int): Boolean = lockedTradeIndices.contains(index)

    fun toggleLock(index: Int): Boolean {
        if (lockedTradeIndices.contains(index)) {
            lockedTradeIndices.remove(index)
            return false
        } else {
            lockedTradeIndices.add(index)
            return true
        }
    }

    fun clearLocks() {
        lockedTradeIndices.clear()
    }

    fun cycleTrades() {
        ClientPlayNetworking.send(CycleTradesPayload(lockedTradeIndices.toList(), false))
    }

    fun requestArmorTrims() {
        ClientPlayNetworking.send(CycleTradesPayload(emptyList(), true))
    }
}
