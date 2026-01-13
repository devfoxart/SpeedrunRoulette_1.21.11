package com.example.examplemod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancementDefinitions {
    public static final Map<String, List<String>> DEFINITIONS = new HashMap<>();

    static {
        // Adventuring Time
        DEFINITIONS.put("minecraft:adventure/adventuring_time", Arrays.asList(
            "minecraft:badlands", "minecraft:bamboo_jungle", "minecraft:beach", "minecraft:birch_forest", 
            "minecraft:cherry_grove", "minecraft:dark_forest", "minecraft:deep_cold_ocean", "minecraft:deep_dark", 
            "minecraft:deep_frozen_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_ocean", "minecraft:desert", 
            "minecraft:dripstone_caves", "minecraft:eroded_badlands", "minecraft:flower_forest", "minecraft:forest", 
            "minecraft:frozen_ocean", "minecraft:frozen_peaks", "minecraft:frozen_river", "minecraft:grove", 
            "minecraft:ice_spikes", "minecraft:jagged_peaks", "minecraft:jungle", "minecraft:lukewarm_ocean", 
            "minecraft:lush_caves", "minecraft:mangrove_swamp", "minecraft:meadow", "minecraft:mushroom_fields", 
            "minecraft:old_growth_birch_forest", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", 
            "minecraft:plains", "minecraft:river", "minecraft:savanna", "minecraft:savanna_plateau", 
            "minecraft:snowy_beach", "minecraft:snowy_plains", "minecraft:snowy_slopes", "minecraft:snowy_taiga", 
            "minecraft:sparse_jungle", "minecraft:stony_peaks", "minecraft:stony_shore", "minecraft:sunflower_plains", 
            "minecraft:swamp", "minecraft:taiga", "minecraft:warm_ocean", "minecraft:windswept_forest", 
            "minecraft:windswept_gravelly_hills", "minecraft:windswept_hills", "minecraft:windswept_savanna", 
            "minecraft:wooded_badlands"
        ));

        // Hot Tourist Destinations (Explore Nether)
        DEFINITIONS.put("minecraft:nether/explore_nether", Arrays.asList(
            "minecraft:nether_wastes", "minecraft:soul_sand_valley", "minecraft:crimson_forest", 
            "minecraft:warped_forest", "minecraft:basalt_deltas"
        ));
        
        // Monsters Hunted
        DEFINITIONS.put("minecraft:adventure/kill_all_mobs", Arrays.asList(
            "minecraft:blaze", "minecraft:cave_spider", "minecraft:creeper", "minecraft:drowned", 
            "minecraft:elder_guardian", "minecraft:ender_dragon", "minecraft:enderman", "minecraft:endermite", 
            "minecraft:evoker", "minecraft:ghast", "minecraft:guardian", "minecraft:hoglin", 
            "minecraft:husk", "minecraft:magma_cube", "minecraft:phantom", "minecraft:piglin", 
            "minecraft:piglin_brute", "minecraft:pillager", "minecraft:ravager", "minecraft:shulker", 
            "minecraft:silverfish", "minecraft:skeleton", "minecraft:slime", "minecraft:spider", 
            "minecraft:stray", "minecraft:vex", "minecraft:vindicator", "minecraft:witch", 
            "minecraft:wither_skeleton", "minecraft:zoglin", "minecraft:zombie", "minecraft:zombie_villager", 
            "minecraft:zombified_piglin"
        ));

        // A Balanced Diet
        DEFINITIONS.put("minecraft:husbandry/balanced_diet", Arrays.asList(
            "minecraft:apple", "minecraft:baked_potato", "minecraft:beef", "minecraft:beetroot", "minecraft:beetroot_soup",
            "minecraft:bread", "minecraft:carrot", "minecraft:chicken", "minecraft:chorus_fruit", "minecraft:cod",
            "minecraft:cooked_beef", "minecraft:cooked_chicken", "minecraft:cooked_cod", "minecraft:cooked_mutton",
            "minecraft:cooked_porkchop", "minecraft:cooked_rabbit", "minecraft:cooked_salmon", "minecraft:cookie",
            "minecraft:dried_kelp", "minecraft:enchanted_golden_apple", "minecraft:glow_berries", "minecraft:golden_apple",
            "minecraft:golden_carrot", "minecraft:honey_bottle", "minecraft:melon_slice", "minecraft:mushroom_stew",
            "minecraft:mutton", "minecraft:poisonous_potato", "minecraft:porkchop", "minecraft:potato", "minecraft:pufferfish",
            "minecraft:pumpkin_pie", "minecraft:rabbit", "minecraft:rabbit_stew", "minecraft:rotten_flesh", "minecraft:salmon",
            "minecraft:spider_eye", "minecraft:suspicious_stew", "minecraft:sweet_berries", "minecraft:tropical_fish"
        ));

        // Two by Two
        DEFINITIONS.put("minecraft:husbandry/bred_all_animals", Arrays.asList(
             "minecraft:armadillo", "minecraft:axolotl", "minecraft:bee", "minecraft:camel", "minecraft:cat", "minecraft:chicken", 
             "minecraft:cow", "minecraft:donkey", "minecraft:fox", "minecraft:frog", "minecraft:goat", "minecraft:hoglin", 
             "minecraft:horse", "minecraft:llama", "minecraft:mooshroom", "minecraft:mule", "minecraft:ocelot", "minecraft:panda", 
             "minecraft:pig", "minecraft:rabbit", "minecraft:sheep", "minecraft:sniffer", "minecraft:strider", "minecraft:trader_llama", 
             "minecraft:turtle", "minecraft:wolf"
        ));

        // A Complete Catalogue
        // Includes both legacy texture paths and modern registry IDs for compatibility
        DEFINITIONS.put("minecraft:husbandry/complete_catalogue", Arrays.asList(
            "minecraft:textures/entity/cat/all_black.png", "minecraft:textures/entity/cat/black.png", "minecraft:textures/entity/cat/british_shorthair.png", 
            "minecraft:textures/entity/cat/calico.png", "minecraft:textures/entity/cat/jellie.png", "minecraft:textures/entity/cat/persian.png", 
            "minecraft:textures/entity/cat/ragdoll.png", "minecraft:textures/entity/cat/red.png", "minecraft:textures/entity/cat/siamese.png", 
            "minecraft:textures/entity/cat/tabby.png", "minecraft:textures/entity/cat/white.png",
            // Modern IDs (Registry Keys)
            "minecraft:all_black", "minecraft:black", "minecraft:british_shorthair", 
            "minecraft:calico", "minecraft:jellie", "minecraft:persian", 
            "minecraft:ragdoll", "minecraft:red", "minecraft:siamese", 
            "minecraft:tabby", "minecraft:white"
        ));
    }
}
