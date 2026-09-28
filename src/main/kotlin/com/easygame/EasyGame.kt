package com.easygame

import com.easygame.command.EasyHopperCommand
import com.easygame.mixin.MerchantMenuAccessor
import com.easygame.network.CycleTradesPayload
import com.easygame.trade.TradeHelper
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.breeze.Breeze
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory

object EasyGame : ModInitializer {
    const val MOD_ID: String = "easygame"
    private val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info("EasyGame initializing: unlimited trades, max enchants, trade cycling, trial chamber boosts, insta hoppers, survival /tp active!")

        // Register custom network payload using serverboundPlay()
        PayloadTypeRegistry.serverboundPlay().register(CycleTradesPayload.TYPE, CycleTradesPayload.STREAM_CODEC)

        // Register server network receiver for cycling/saving trades & loading trims
        ServerPlayNetworking.registerGlobalReceiver(CycleTradesPayload.TYPE) { payload, context ->
            val player = context.player()
            context.server().execute {
                val menu = player.containerMenu
                if (menu is MerchantMenu) {
                    val trader = (menu as MerchantMenuAccessor).trader
                    if (trader != null) {
                        if (payload.loadTrims) {
                            TradeHelper.loadAllArmorTrims(trader, player)
                        } else {
                            TradeHelper.cycleTrades(trader, payload.lockedIndices, player)
                        }
                    }
                }
            }
        }

        // Commands: /easyhopper
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            EasyHopperCommand.register(dispatcher)
        }

        // Breeze drops 10 Ominous Trial Keys on death
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (entity is Breeze && !entity.level().isClientSide) {
                val serverLevel = entity.level() as? ServerLevel
                if (serverLevel != null) {
                    entity.spawnAtLocation(serverLevel, ItemStack(Items.OMINOUS_TRIAL_KEY, 10))
                }
            }
        }
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
