package com.easygame

import com.easygame.command.EasyHopperCommand
import com.easygame.mixin.BaseSpawnerAccessor
import com.easygame.mixin.MerchantMenuAccessor
import com.easygame.network.CycleTradesPayload
import com.easygame.trade.TradeHelper
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.InclusiveRange
import net.minecraft.util.random.WeightedList
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.breeze.Breeze
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.level.SpawnData
import net.minecraft.world.level.block.entity.SpawnerBlockEntity
import net.minecraft.world.level.gameevent.GameEvent
import org.slf4j.LoggerFactory
import java.util.Optional

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

        // Mob death drops: Breeze trial keys & 40% chance for mob spawn eggs
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            val level = entity.level()
            if (!level.isClientSide && level is ServerLevel) {
                if (entity is Breeze) {
                    entity.spawnAtLocation(level, ItemStack(Items.OMINOUS_TRIAL_KEY, 10))
                }

                if (entity is Mob) {
                    if (level.random.nextFloat() < 0.40f) {
                        SpawnEggItem.byId(entity.type).ifPresent { eggHolder ->
                            entity.spawnAtLocation(level, ItemStack(eggHolder.value()))
                        }
                    }
                }
            }
        }

        // Spawner interaction: Spawn egg binding OR Block Generator inspection & collection
        UseBlockCallback.EVENT.register { player, level, hand, hitResult ->
            val pos = hitResult.blockPos
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is SpawnerBlockEntity) {
                val stack = player.getItemInHand(hand)
                if (stack.item is SpawnEggItem) {
                    val entityType = SpawnEggItem.getType(stack)
                    if (entityType != null) {
                        if (!level.isClientSide && level is ServerLevel) {
                            val entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()
                            val entityTag = CompoundTag().apply {
                                putString("id", entityKey)
                            }
                            val rules = SpawnData.CustomSpawnRules(InclusiveRange(0, 15), InclusiveRange(0, 15))
                            val spawnData = SpawnData(entityTag, Optional.of(rules), Optional.empty())

                            val baseSpawner = blockEntity.spawner
                            val accessor = baseSpawner as BaseSpawnerAccessor
                            accessor.setNextSpawnData(spawnData)
                            accessor.setSpawnPotentials(WeightedList.of(spawnData))
                            accessor.setSpawnDelay(20)
                            accessor.setMinSpawnDelay(100)
                            accessor.setMaxSpawnDelay(300)
                            accessor.setSpawnCount(4)
                            accessor.setRequiredPlayerRange(32)
                            accessor.setDisplayEntity(null)

                            blockEntity.setChanged()
                            val state = level.getBlockState(pos)
                            level.sendBlockUpdated(pos, state, state, 3)
                            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos)
                            level.playSound(null, pos, SoundEvents.SPAWNER_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f)

                            player.sendSystemMessage(
                                Component.literal("§6[EasyGame] §aSpawner bound to §e${entityType.description.string} §a(Unlimited)!")
                            )
                        }
                        player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true)
                        return@register InteractionResult.SUCCESS
                    }
                } else {
                    // Block Generator right-click to inspect and collect
                    if (!level.isClientSide && level is ServerLevel && player is ServerPlayer) {
                        com.easygame.generator.GeneratorStateManager.handleRightClick(player, level, pos)
                    }
                    player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true)
                    return@register InteractionResult.SUCCESS
                }
            }
            InteractionResult.PASS
        }

        // Drop stored generator items when spawner is broken
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register { world, _, pos, state, _ ->
            if (!world.isClientSide && world is ServerLevel && state.`is`(net.minecraft.world.level.block.Blocks.SPAWNER)) {
                com.easygame.generator.GeneratorStateManager.onSpawnerBroken(world, pos)
            }
        }
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
