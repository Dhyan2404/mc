package com.easygame.mixin;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract void setHealth(float health);

    @Shadow
    public abstract boolean removeAllEffects();

    @Shadow
    public abstract boolean addEffect(MobEffectInstance effect);

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), cancellable = true)
    private void onCheckTotemDeathProtection(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        // If vanilla already found a totem in main/offhand, keep true
        if (cir.getReturnValue()) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player) {
            // Check player inventory for Totem of Undying or any item with DeathProtection component
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;

                DeathProtection deathProtection = stack.get(DataComponents.DEATH_PROTECTION);
                boolean isTotemItem = stack.is(Items.TOTEM_OF_UNDYING);

                if (deathProtection != null || isTotemItem) {
                    ItemStack totemCopy = stack.copy();
                    stack.shrink(1);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.awardStat(Stats.ITEM_USED.get(totemCopy.getItem()));
                        CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, totemCopy);
                        totemCopy.causeUseVibration(entity, GameEvent.ITEM_INTERACT_FINISH);
                        serverPlayer.sendSystemMessage(
                                Component.literal("§6§l✦ §eInventory Totem activated! §aYou were saved from death! §6§l✦")
                        );
                    }

                    this.setHealth(1.0F);

                    if (deathProtection != null) {
                        deathProtection.applyEffects(totemCopy, entity);
                    } else {
                        this.removeAllEffects();
                        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                        this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                    }

                    // 35 = Totem animation & sound
                    this.level().broadcastEntityEvent(entity, (byte) 35);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
