package com.easygame.trade;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VillagerTradeRegistrar {

    public static class SimpleListing implements VillagerTrades.ItemListing {
        private final ItemCost costA;
        private final Optional<ItemCost> costB;
        private final ItemStack result;
        private final int maxUses;
        private final int xp;

        public SimpleListing(ItemCost costA, ItemStack result, int maxUses, int xp) {
            this(costA, Optional.empty(), result, maxUses, xp);
        }

        public SimpleListing(ItemCost costA, Optional<ItemCost> costB, ItemStack result, int maxUses, int xp) {
            this.costA = costA;
            this.costB = costB;
            this.result = result;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            ItemStack res = result.copy();
            TradeHelper.maximizeEnchantments(res);
            return new MerchantOffer(costA, costB, res, 0, maxUses, xp, 0.05f);
        }
    }

    public static void registerTrades() {
        // Register Fletcher Trades:
        // 1. 1 Stick -> 1 Emerald
        // 2. 1 Stick -> 2 Apples
        Int2ObjectMap<VillagerTrades.ItemListing[]> fletcherTrades = VillagerTrades.TRADES.get(VillagerProfession.FLETCHER);
        if (fletcherTrades != null) {
            VillagerTrades.ItemListing[] level1 = fletcherTrades.get(1);
            List<VillagerTrades.ItemListing> list = new ArrayList<>();
            list.add(new SimpleListing(new ItemCost(Items.STICK, 1), new ItemStack(Items.EMERALD, 1), 999999, 2));
            list.add(new SimpleListing(new ItemCost(Items.STICK, 1), new ItemStack(Items.APPLE, 2), 999999, 2));
            if (level1 != null) {
                for (VillagerTrades.ItemListing oldListing : level1) {
                    // Filter out old stick trade
                    list.add(oldListing);
                }
            }
            fletcherTrades.put(1, list.toArray(new VillagerTrades.ItemListing[0]));
        }

        // Register Toolsmith Armor Trim Trades:
        // Cost: 2 Diamonds -> 1 Armor Trim
        Int2ObjectMap<VillagerTrades.ItemListing[]> toolsmithTrades = VillagerTrades.TRADES.get(VillagerProfession.TOOLSMITH);
        if (toolsmithTrades != null) {
            for (int lvl = 1; lvl <= 5; lvl++) {
                VillagerTrades.ItemListing[] existing = toolsmithTrades.get(lvl);
                List<VillagerTrades.ItemListing> list = new ArrayList<>();
                if (existing != null) {
                    list.addAll(Arrays.asList(existing));
                }
                // Distribute armor trim templates across levels
                int startIdx = (lvl - 1) * 4;
                int endIdx = Math.min(startIdx + 4, TradeHelper.ARMOR_TRIM_TEMPLATES.size());
                for (int i = startIdx; i < endIdx; i++) {
                    Item trimItem = TradeHelper.ARMOR_TRIM_TEMPLATES.get(i);
                    list.add(new SimpleListing(new ItemCost(Items.DIAMOND, 2), new ItemStack(trimItem, 1), 999999, 5));
                }
                toolsmithTrades.put(lvl, list.toArray(new VillagerTrades.ItemListing[0]));
            }
        }
    }
}
