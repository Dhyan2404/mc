package com.easygame.client

object EnchantmentDescriptions {
    private val DESCRIPTIONS = mapOf(
        "protection" to "Reduces all incoming damage by 4% per level (max 16% per piece).",
        "fire_protection" to "Reduces fire damage by 8% per level and shortens burn duration.",
        "feather_falling" to "Reduces fall damage and Ender Pearl damage by 12% per level.",
        "blast_protection" to "Reduces explosion damage by 8% per level and explosion knockback.",
        "projectile_protection" to "Reduces projectile damage (arrows, fireballs, tridents) by 8% per level.",
        "respiration" to "Extends underwater breathing time by 15s per level and reduces drowning damage.",
        "aqua_affinity" to "Increases underwater block breaking speed to match normal land speed.",
        "thorns" to "Deals damage back to attackers with a chance based on level.",
        "depth_strider" to "Increases underwater movement speed by 33% per level.",
        "frost_walker" to "Freezes water blocks underneath you into frosted ice as you walk.",
        "soul_speed" to "Boosts movement speed on Soul Sand and Soul Soil by 30% + 10% per level.",
        "swift_sneak" to "Increases crouching movement speed by 15% per level (up to 75%).",
        "sharpness" to "Increases melee attack damage by 1.25 per level.",
        "smite" to "Increases damage dealt to undead mobs (zombies, skeletons, withers) by 2.5 per level.",
        "bane_of_arthropods" to "Increases damage to spiders, silverfish, and bees, applying Slowness.",
        "knockback" to "Increases attack knockback distance by 3 blocks per level.",
        "fire_aspect" to "Ignites attacked targets on fire for 4 seconds per level.",
        "looting" to "Increases mob drop amounts and rare loot drop chances.",
        "sweeping_edge" to "Increases sweep attack damage.",
        "efficiency" to "Increases mining speed by +1 + (level * level) and axe shield disable.",
        "silk_touch" to "Causes mined blocks to drop as themselves rather than normal drops.",
        "unbreaking" to "Gives a chance for item durability not to decrease upon use.",
        "fortune" to "Increases ore, crop, and block drop counts.",
        "power" to "Increases bow arrow damage by 25% * (level + 1).",
        "punch" to "Increases bow arrow knockback distance.",
        "flame" to "Arrows shot from this bow are lit on fire and ignite targets.",
        "infinity" to "Shooting regular arrows does not consume arrows from your inventory.",
        "luck_of_the_sea" to "Increases chance of catching valuable treasure while fishing.",
        "lure" to "Decreases wait time until a fish bites the hook by 5s per level.",
        "loyalty" to "Causes thrown trident to return to owner upon impact.",
        "impaling" to "Increases trident damage dealt to aquatic mobs and rain-soaked targets.",
        "riptide" to "Launches player forward when throwing trident in water or rain.",
        "channeling" to "Strikes lightning on target entity when thrown during thunderstorms.",
        "multishot" to "Shoots 3 arrows or fireworks in a spread for the cost of 1.",
        "quick_charge" to "Decreases crossbow reloading time by 0.25 seconds per level.",
        "piercing" to "Crossbow arrows pierce through entities and ignore shields.",
        "density" to "Increases Mace smash attack damage by 0.5 per block fallen per level.",
        "breach" to "Reduces target armor effectiveness against Mace attacks by 15% per level.",
        "wind_burst" to "Emits a wind burst upon falling Mace smash attack, launching you upward.",
        "mending" to "Repairs item durability using collected experience orbs (2 durability per XP).",
        "binding_curse" to "Armor item cannot be removed from equipment slot once put on.",
        "vanishing_curse" to "Item completely disappears upon death instead of dropping.",
        "soul_speed" to "Boosts movement speed on Soul Sand and Soul Soil by 30% + 10% per level.",
        "swift_sneak" to "Increases crouching movement speed by 15% per level (up to 75%)."
    )

    fun getDescription(enchantmentPath: String): String? {
        val cleanKey = enchantmentPath.lowercase().substringAfterLast(":")
        return DESCRIPTIONS[cleanKey]
    }
}
