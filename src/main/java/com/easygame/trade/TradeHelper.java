package com.easygame.trade;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
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
     * Loads all 18 armor trim trades into the villager.
     */
    public static void loadAllArmorTrims(Villager villager, ServerPlayer player) {
        MerchantOffers offers = villager.getOffers();
        offers.clear();
        for (Item trimItem : ARMOR_TRIM_TEMPLATES) {
            offers.add(createArmorTrimOffer(trimItem));
        }
        player.sendMerchantOffers(
                player.containerMenu.containerId,
                offers,
                villager.getVillagerData().getLevel(),
                villager.getVillagerXp(),
                villager.showProgressBar(),
                villager.canRestock()
        );
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Cycles / refreshes unlocked trades while preserving all locked/saved trades.
     */
    public static void cycleTrades(Villager villager, List<Integer> lockedIndices, ServerPlayer player) {
        MerchantOffers currentOffers = villager.getOffers();
        Map<Integer, MerchantOffer> preservedMap = new HashMap<>();

        for (int idx : lockedIndices) {
            if (idx >= 0 && idx < currentOffers.size()) {
                preservedMap.put(idx, currentOffers.get(idx));
            }
        }

        VillagerData data = villager.getVillagerData();
        VillagerProfession profession = data.getProfession();
        int villagerLevel = Math.max(1, data.getLevel());

        var tradePool = VillagerTrades.TRADES.get(profession);
        MerchantOffers newOffers = new MerchantOffers();
        RandomSource random = villager.getRandom();

        if (tradePool != null) {
            for (int lvl = 1; lvl <= villagerLevel; lvl++) {
                VillagerTrades.ItemListing[] listings = tradePool.get(lvl);
                if (listings != null && listings.length > 0) {
                    List<VillagerTrades.ItemListing> shuffled = new ArrayList<>(Arrays.asList(listings));
                    Collections.shuffle(shuffled, new Random(random.nextLong()));
                    int pickCount = Math.min(2, shuffled.size());
                    for (int i = 0; i < pickCount; i++) {
                        MerchantOffer rawOffer = shuffled.get(i).getOffer(villager, random);
                        if (rawOffer != null) {
                            ItemStack result = rawOffer.getResult().copy();
                            maximizeEnchantments(result);

                            ItemCost costA = rawOffer.getItemCostA();
                            Optional<ItemCost> costB = rawOffer.getItemCostB();

                            // If enchanted book, remove secondary cost (no book needed, only emeralds)
                            if (result.is(Items.ENCHANTED_BOOK)) {
                                costB = Optional.empty();
                            }

                            MerchantOffer cleanOffer = new MerchantOffer(
                                    costA,
                                    costB,
                                    result,
                                    0,
                                    999999, // unlimited trades
                                    rawOffer.getXp(),
                                    rawOffer.getPriceMultiplier()
                            );
                            newOffers.add(cleanOffer);
                        }
                    }
                }
            }
        }

        // Merge preserved/locked trades back into their original positions
        MerchantOffers finalOffers = new MerchantOffers();
        int maxSlots = Math.max(currentOffers.size(), newOffers.size());
        int newIdx = 0;

        for (int i = 0; i < maxSlots; i++) {
            if (preservedMap.containsKey(i)) {
                finalOffers.add(preservedMap.get(i));
            } else if (newIdx < newOffers.size()) {
                finalOffers.add(newOffers.get(newIdx++));
            } else if (i < currentOffers.size()) {
                finalOffers.add(currentOffers.get(i));
            }
        }

        currentOffers.clear();
        currentOffers.addAll(finalOffers);

        player.sendMerchantOffers(
                player.containerMenu.containerId,
                currentOffers,
                villager.getVillagerData().getLevel(),
                villager.getVillagerXp(),
                villager.showProgressBar(),
                villager.canRestock()
        );

        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
