package com.easygame

import com.easygame.mixin.MerchantMenuAccessor
import com.easygame.network.CycleTradesPayload
import com.easygame.trade.TradeHelper
import com.easygame.trade.VillagerTradeRegistrar
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.monster.breeze.Breeze
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory

object EasyGame : ModInitializer {
    const val MOD_ID: String = "easygame"
    private val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info("EasyGame initializing: unlimited trades, max enchants, trade cycling, trial chamber boosts active!")

        // Register custom network payload
        PayloadTypeRegistry.playC2S().register(CycleTradesPayload.TYPE, CycleTradesPayload.STREAM_CODEC)

        // Register server network receiver for cycling/saving trades & loading trims
        ServerPlayNetworking.registerGlobalReceiver(CycleTradesPayload.TYPE) { payload, context ->
            val player = context.player()
            context.server().execute {
                val menu = player.containerMenu
                if (menu is MerchantMenu) {
                    val trader = (menu as MerchantMenuAccessor).trader
                    if (trader is Villager) {
                        if (payload.loadTrims) {
                            TradeHelper.loadAllArmorTrims(trader, player)
                        } else {
                            TradeHelper.cycleTrades(trader, payload.lockedIndices, player)
                        }
                    }
                }
            }
        }

        // Register customized villager trades (fletcher 1 stick -> 1 emerald, 1 stick -> 2 apples, toolsmith trims)
        VillagerTradeRegistrar.registerTrades()

        // Breeze drops 10 Ominous Trial Keys on death
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (entity is Breeze && !entity.level().isClientSide) {
                entity.spawnAtLocation(ItemStack(Items.OMINOUS_TRIAL_KEY, 10))
            }
        }
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
