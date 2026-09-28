package com.easygame.trade;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
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

    /**
     * Maximize any enchantments present on the item stack to their highest level.
     */
    public static void maximizeEnchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        // Check Stored Enchantments (e.g. Enchanted Books)
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(stored);
            for (Holder<Enchantment> holder : stored.keySet()) {
                int maxLevel = holder.value().getMaxLevel();
                mutable.set(holder, maxLevel);
            }
            stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        }

        // Check regular item enchantments (e.g. Swords, Tools, Armor)
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

    /**
     * Creates an armor trim trade costing 2 Diamonds for 1 trim template.
     */
    public static MerchantOffer createArmorTrimOffer(Item trimItem) {
        ItemCost cost = new ItemCost(Items.DIAMOND, 2);
        ItemStack result = new ItemStack(trimItem, 1);
        return new MerchantOffer(cost, Optional.empty(), result, 0, 999999, 5, 0.05f);
    }

    /**
     * Loads all 18 armor trim trades into the merchant.
     */
    public static void loadAllArmorTrims(Merchant trader, ServerPlayer player) {
        MerchantOffers offers = trader.getOffers();
        offers.clear();
        for (Item trimItem : ARMOR_TRIM_TEMPLATES) {
            offers.add(createArmorTrimOffer(trimItem));
        }
        syncOffers(trader, player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Cycles / refreshes unlocked trades while preserving all locked/saved trades.
     */
    public static void cycleTrades(Merchant trader, List<Integer> lockedIndices, ServerPlayer player) {
        MerchantOffers currentOffers = trader.getOffers();
        Map<Integer, MerchantOffer> preservedMap = new HashMap<>();

        for (int idx : lockedIndices) {
            if (idx >= 0 && idx < currentOffers.size()) {
                preservedMap.put(idx, currentOffers.get(idx));
            }
        }

        int targetTradeCount = Math.max(2, currentOffers.size());
        List<MerchantOffer> freshOffers = generateFreshTrades(trader, player, targetTradeCount);

        MerchantOffers finalOffers = new MerchantOffers();
        int freshIdx = 0;

        for (int i = 0; i < targetTradeCount; i++) {
            if (preservedMap.containsKey(i)) {
                // Keep the saved/locked trade intact
                finalOffers.add(preservedMap.get(i));
            } else if (freshIdx < freshOffers.size()) {
                finalOffers.add(freshOffers.get(freshIdx++));
            }
        }

        currentOffers.clear();
        currentOffers.addAll(finalOffers);

        syncOffers(trader, player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Ensures Fletcher trades strictly follow:
     * - Sell: 1 Stick -> 1 Emerald
     * - Buy: 1 Stick -> 2 Apples
     * - Removes any conflicting stick trades (e.g. 32 sticks -> 1 emerald)
     */
    public static void customizeVillagerOffers(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        if (offers == null) return;

        // Maximize all enchantments
        for (MerchantOffer offer : offers) {
            maximizeEnchantments(offer.getResult());
        }

        if (villager.getVillagerData().profession().is(VillagerProfession.FLETCHER)) {
            // Remove conflicting stick trades
            offers.removeIf(offer -> {
                ItemStack cost = offer.getCostA();
                ItemStack res = offer.getResult();
                if (cost.is(Items.STICK) && res.is(Items.EMERALD)) return true;
                if (cost.is(Items.STICK) && res.is(Items.APPLE)) return true;
                return false;
            });

            // 1 Stick -> 1 Emerald
            offers.add(0, new MerchantOffer(
                    new ItemCost(Items.STICK, 1),
                    Optional.empty(),
                    new ItemStack(Items.EMERALD, 1),
                    0, 999999, 2, 0.05f
            ));

            // 1 Stick -> 2 Apples
            offers.add(1, new MerchantOffer(
                    new ItemCost(Items.STICK, 1),
                    Optional.empty(),
                    new ItemStack(Items.APPLE, 2),
                    0, 999999, 2, 0.05f
            ));
        }
    }

    /**
     * Generates fresh trades depending on villager profession.
     */
    private static List<MerchantOffer> generateFreshTrades(Merchant trader, ServerPlayer player, int count) {
        List<MerchantOffer> list = new ArrayList<>();
        Random random = new Random();

        if (trader instanceof Villager villager) {
            if (villager.getVillagerData().profession().is(VillagerProfession.FLETCHER)) {
                list.add(new MerchantOffer(new ItemCost(Items.STICK, 1), Optional.empty(), new ItemStack(Items.EMERALD, 1), 0, 999999, 2, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.STICK, 1), Optional.empty(), new ItemStack(Items.APPLE, 2), 0, 999999, 2, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.ARROW, 16), 0, 999999, 2, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), Optional.empty(), new ItemStack(Items.BOW, 1), 0, 999999, 2, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), Optional.empty(), new ItemStack(Items.CROSSBOW, 1), 0, 999999, 2, 0.05f));
                return list;
            }

            if (villager.getVillagerData().profession().is(VillagerProfession.TOOLSMITH)) {
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.DIAMOND_PICKAXE, 1), 0, 999999, 5, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.DIAMOND_AXE, 1), 0, 999999, 5, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.DIAMOND_SHOVEL, 1), 0, 999999, 5, 0.05f));
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.DIAMOND_HOE, 1), 0, 999999, 5, 0.05f));
                return list;
            }
        }

        // Default & Librarian: Max level enchanted books costing strictly emeralds only (no regular books!)
        try {
            var lookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            List<Holder.Reference<Enchantment>> enchants = new ArrayList<>(lookup.listElements().toList());
            Collections.shuffle(enchants, random);

            for (var holder : enchants) {
                if (list.size() >= count + 4) break;

                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutable.set(holder, holder.value().getMaxLevel());
                book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

                int emeraldCost = 5 + random.nextInt(20);
                list.add(new MerchantOffer(new ItemCost(Items.EMERALD, emeraldCost), Optional.empty(), book, 0, 999999, 5, 0.05f));
            }
        } catch (Exception e) {
            // Fallback
            list.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.BOOKSHELF, 1), 0, 999999, 2, 0.05f));
        }

        return list;
    }

    private static void syncOffers(Merchant trader, ServerPlayer player) {
        int level = 1;
        if (trader instanceof Villager villager) {
            level = villager.getVillagerData().level();
        }
        player.sendMerchantOffers(
                player.containerMenu.containerId,
                trader.getOffers(),
                level,
                trader.getVillagerXp(),
                trader.showProgressBar(),
                trader.canRestock()
        );
    }
}
