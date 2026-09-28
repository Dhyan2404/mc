package com.easygame.trade;

import com.easygame.mixin.MerchantMenuAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.*;

public class TradeHelper {

    public static final List<Item> ARMOR_TRIM_TEMPLATES = List.of(
            Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE
    );

    public static void maximizeEnchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(stored);
            for (Holder<Enchantment> holder : stored.keySet()) {
                int maxLevel = holder.value().getMaxLevel();
                mutable.set(holder, maxLevel);
            }
            stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        }

        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchants);
            for (Holder<Enchantment> holder : enchants.keySet()) {
                int maxLevel = holder.value().getMaxLevel();
                mutable.set(holder, maxLevel);
            }
            stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        }
    }

    public static MerchantOffer createArmorTrimOffer(Item trimItem) {
        ItemCost cost = new ItemCost(Items.DIAMOND, 2);
        ItemStack result = new ItemStack(trimItem, 1);
        return new MerchantOffer(cost, Optional.empty(), result, 0, 999999, 5, 0.05f);
    }

    public static void loadAllArmorTrims(Merchant trader, ServerPlayer player) {
        MerchantOffers offers = trader.getOffers();
        if (offers == null) {
            offers = new MerchantOffers();
            trader.overrideOffers(offers);
        }
        offers.clear();
        for (Item trimItem : ARMOR_TRIM_TEMPLATES) {
            offers.add(createArmorTrimOffer(trimItem));
        }
        if (player.containerMenu instanceof MerchantMenu menu) {
            menu.setOffers(offers);
            try {
                menu.slotsChanged(((MerchantMenuAccessor) menu).getTradeContainer());
            } catch (Throwable ignored) {
            }
        }
        syncOffers(trader, player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void cycleTrades(Merchant trader, List<Integer> lockedIndices, ServerPlayer player) {
        MerchantOffers currentOffers = trader.getOffers();
        if (currentOffers == null) {
            currentOffers = new MerchantOffers();
            trader.overrideOffers(currentOffers);
        }

        // Snapshot existing offers to safely preserve locked ones
        MerchantOffers oldOffers = new MerchantOffers();
        for (MerchantOffer o : currentOffers) {
            oldOffers.add(o.copy());
        }

        Map<Integer, MerchantOffer> preservedMap = new HashMap<>();
        if (lockedIndices != null) {
            for (int idx : lockedIndices) {
                if (idx >= 0 && idx < oldOffers.size()) {
                    preservedMap.put(idx, oldOffers.get(idx));
                }
            }
        }

        int targetTradeCount;
        if (!preservedMap.isEmpty()) {
            // Locked trades exist -> add +1 trade slot
            targetTradeCount = Math.max(oldOffers.size() + 1, 2);
        } else {
            // No locked trades -> reset to 2 fresh trades
            targetTradeCount = 2;
        }

        int neededFresh = Math.max(targetTradeCount - preservedMap.size(), 1);
        List<MerchantOffer> freshOffers = generateFreshTrades(trader, player, oldOffers, neededFresh + 4);

        MerchantOffers finalOffers = new MerchantOffers();
        int freshIdx = 0;

        for (int i = 0; i < targetTradeCount; i++) {
            if (preservedMap.containsKey(i)) {
                finalOffers.add(preservedMap.get(i));
            } else if (freshIdx < freshOffers.size()) {
                finalOffers.add(freshOffers.get(freshIdx++));
            }
        }

        while (finalOffers.size() < targetTradeCount && freshIdx < freshOffers.size()) {
            finalOffers.add(freshOffers.get(freshIdx++));
        }

        for (MerchantOffer fallback : oldOffers) {
            if (finalOffers.size() >= targetTradeCount) break;
            finalOffers.add(fallback.copy());
        }

        currentOffers.clear();
        currentOffers.addAll(finalOffers);
        trader.overrideOffers(currentOffers);

        if (player.containerMenu instanceof MerchantMenu menu) {
            menu.setOffers(currentOffers);
            try {
                menu.slotsChanged(((MerchantMenuAccessor) menu).getTradeContainer());
            } catch (Throwable ignored) {
            }
        }

        syncOffers(trader, player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void customizeVillagerOffers(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        if (offers == null) return;

        for (MerchantOffer offer : offers) {
            maximizeEnchantments(offer.getResult());
        }

        if (villager.getVillagerData().profession().is(VillagerProfession.FLETCHER)) {
            boolean hasStickEmerald = false;
            boolean hasStickApple = false;
            for (MerchantOffer offer : offers) {
                if (offer.getCostA().is(Items.STICK) && offer.getCostA().getCount() == 1) {
                    if (offer.getResult().is(Items.EMERALD)) hasStickEmerald = true;
                    if (offer.getResult().is(Items.APPLE)) hasStickApple = true;
                }
            }

            if (!hasStickEmerald) {
                offers.add(0, new MerchantOffer(
                        new ItemCost(Items.STICK, 1),
                        Optional.empty(),
                        new ItemStack(Items.EMERALD, 1),
                        0, 999999, 2, 0.05f
                ));
            }
            if (!hasStickApple) {
                int insertPos = Math.min(1, offers.size());
                offers.add(insertPos, new MerchantOffer(
                        new ItemCost(Items.STICK, 1),
                        Optional.empty(),
                        new ItemStack(Items.APPLE, 2),
                        0, 999999, 2, 0.05f
                ));
            }
        }
    }

    private static List<MerchantOffer> generateFreshTrades(Merchant trader, ServerPlayer player, MerchantOffers oldOffers, int count) {
        List<MerchantOffer> list = new ArrayList<>();
        Random random = new Random();

        if (trader instanceof Villager villager) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            VillagerData data = villager.getVillagerData();
            Holder<VillagerProfession> profHolder = data.profession();

            // 1. LIBRARIAN: Rolls max-level enchanted books for emeralds
            if (profHolder.is(VillagerProfession.LIBRARIAN)) {
                generateEnchantedBookTrades(list, serverLevel, count, random);
                return list;
            }

            // 2. FLETCHER: Pinned stick -> emerald and stick -> apple, plus authentic bows/arrows
            if (profHolder.is(VillagerProfession.FLETCHER)) {
                list.add(new MerchantOffer(new ItemCost(Items.STICK, 1), Optional.empty(), new ItemStack(Items.EMERALD, 1), 0, 999999, 2, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.STICK, 1), Optional.empty(), new ItemStack(Items.APPLE, 2), 0, 999999, 2, 0.05f));

                ItemStack bow = new ItemStack(Items.BOW);
                maximizeEnchantments(bow);
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), Optional.empty(), bow, 0, 999999, 5, 0.05f));

                ItemStack crossbow = new ItemStack(Items.CROSSBOW);
                maximizeEnchantments(crossbow);
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), Optional.empty(), crossbow, 0, 999999, 5, 0.05f));

                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.ARROW, 16), 0, 999999, 2, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.SPECTRAL_ARROW, 8), 0, 999999, 2, 0.05f));
                return list;
            }

            // 3. ALL OTHER PROFESSIONS (Carpenter, Farmer, Armorer, Toolsmith, Cleric, etc.):
            // Roll authentic profession trades safely using native villager trade evaluation
            rollAuthenticTrades(villager, oldOffers, list, count);
            return list;
        }

        // Non-villager trader (e.g. Wandering Trader)
        if (oldOffers != null) {
            for (MerchantOffer o : oldOffers) {
                if (list.size() >= count) break;
                list.add(o.copy());
            }
        }
        return list;
    }

    private static void rollAuthenticTrades(Villager villager, MerchantOffers oldOffers, List<MerchantOffer> list, int count) {
        for (int roll = 0; roll < 4 && list.size() < count; roll++) {
            villager.setOffers(null);
            MerchantOffers rolled = villager.getOffers();
            if (rolled != null && !rolled.isEmpty()) {
                for (MerchantOffer o : rolled) {
                    if (list.size() >= count) break;
                    MerchantOffer copy = o.copy();
                    maximizeEnchantments(copy.getResult());

                    boolean duplicate = false;
                    for (MerchantOffer existing : list) {
                        if (ItemStack.matches(existing.getResult(), copy.getResult()) &&
                            ItemStack.matches(existing.getCostA(), copy.getCostA())) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        list.add(copy);
                    }
                }
            }
        }

        if (oldOffers != null) {
            for (MerchantOffer o : oldOffers) {
                if (list.size() >= count) break;
                MerchantOffer copy = o.copy();
                maximizeEnchantments(copy.getResult());
                boolean duplicate = false;
                for (MerchantOffer existing : list) {
                    if (ItemStack.matches(existing.getResult(), copy.getResult()) &&
                        ItemStack.matches(existing.getCostA(), copy.getCostA())) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    list.add(copy);
                }
            }
        }

        if (list.isEmpty()) {
            list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.IRON_INGOT, 4), 0, 999999, 2, 0.05f));
        }
    }

    private static void generateEnchantedBookTrades(List<MerchantOffer> list, ServerLevel serverLevel, int count, Random random) {
        try {
            var lookup = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            List<Holder.Reference<Enchantment>> enchants = new ArrayList<>(lookup.listElements().toList());
            Collections.shuffle(enchants, random);

            for (var holder : enchants) {
                if (list.size() >= count) break;

                ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(holder, holder.value().getMaxLevel()));
                int emeraldCost = 5 + random.nextInt(20);
                list.add(new MerchantOffer(
                        new ItemCost(Items.EMERALD, emeraldCost),
                        Optional.empty(),
                        book,
                        0,
                        999999,
                        5,
                        0.05f
                ));
            }
        } catch (Exception e) {
            list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.BOOKSHELF, 1), 0, 999999, 2, 0.05f));
        }
    }

    private static void syncOffers(Merchant trader, ServerPlayer player) {
        int level = 1;
        if (trader instanceof Villager villager) {
            level = villager.getVillagerData().level();
        }
        MerchantOffers offers = trader.getOffers();
        player.sendMerchantOffers(
                player.containerMenu.containerId,
                offers,
                level,
                trader.getVillagerXp(),
                trader.showProgressBar(),
                trader.canRestock()
        );
        trader.setTradingPlayer(player);
    }
}
