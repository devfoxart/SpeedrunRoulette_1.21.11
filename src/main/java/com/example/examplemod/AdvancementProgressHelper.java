package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;

public class AdvancementProgressHelper {
    public record CriteriaInfo(String rawName, Component displayName, boolean completed) {}

    private static Field progressField;

    static {
        try {
            // Try standard name first
            try {
                progressField = ClientAdvancements.class.getDeclaredField("progress");
            } catch (NoSuchFieldException e) {
                // Try searching by type Map<AdvancementHolder, AdvancementProgress>
                for (Field f : ClientAdvancements.class.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        progressField = f;
                        break;
                    }
                }
            }
            
            if (progressField != null) {
                progressField.setAccessible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void requestUpdate(String advancementId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        
        net.minecraft.resources.Identifier targetId = net.minecraft.resources.Identifier.tryParse(advancementId);
        if (targetId == null) return;
        
        // Find the root advancement to send OPENED_TAB
        net.minecraft.resources.Identifier rootId = targetId;
        
        try {
            ClientAdvancements advancements = mc.player.connection.getAdvancements();
            AdvancementHolder current = advancements.get(targetId);
            
            if (current != null) {
                // Walk up to find root
                while (true) {
                    // Access parent safely
                    java.util.Optional<net.minecraft.resources.Identifier> parentOpt = current.value().parent();
                    if (parentOpt.isEmpty()) break;
                    
                    net.minecraft.resources.Identifier parentId = parentOpt.get();
                    AdvancementHolder parentHolder = advancements.get(parentId);
                    if (parentHolder == null) break;
                    
                    current = parentHolder;
                }
                rootId = current.id();
            } else {
                // Client doesn't know this advancement yet (tree not loaded)
                // We must guess the root to request it
                String path = targetId.getPath();
                if (path.startsWith("story/")) rootId = net.minecraft.resources.Identifier.tryParse("minecraft:story/root");
                else if (path.startsWith("nether/")) rootId = net.minecraft.resources.Identifier.tryParse("minecraft:nether/root");
                else if (path.startsWith("end/")) rootId = net.minecraft.resources.Identifier.tryParse("minecraft:end/root");
                else if (path.startsWith("adventure/")) rootId = net.minecraft.resources.Identifier.tryParse("minecraft:adventure/root");
                else if (path.startsWith("husbandry/")) rootId = net.minecraft.resources.Identifier.tryParse("minecraft:husbandry/root");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (rootId != null) {
            mc.getConnection().send(new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.OPENED_TAB, rootId));
        }
    }

    public static List<CriteriaInfo> getCriteria(String advancementId) {
        List<CriteriaInfo> result = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return result;
        
        ClientAdvancements advancements = mc.player.connection.getAdvancements();
        net.minecraft.resources.Identifier targetId = net.minecraft.resources.Identifier.tryParse(advancementId);
        if (targetId == null) return result;

        Set<String> completedSet = new HashSet<>();
        boolean foundInGame = false;

        // Try SinglePlayer Server bypass if client data is missing
        if (!foundInGame && mc.getSingleplayerServer() != null) {
            try {
                net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
                net.minecraft.server.level.ServerPlayer serverPlayer = server.getPlayerList().getPlayer(mc.player.getUUID());
                
                if (serverPlayer != null) {
                    net.minecraft.server.PlayerAdvancements serverAdvancements = serverPlayer.getAdvancements();
                    net.minecraft.server.ServerAdvancementManager manager = server.getAdvancements();
                    net.minecraft.advancements.AdvancementHolder serverHolder = manager.get(targetId);
                    
                    if (serverHolder != null) {
                        net.minecraft.advancements.AdvancementProgress serverProgress = serverAdvancements.getOrStartProgress(serverHolder);
                        
                        Iterable<String> completedIter = serverProgress.getCompletedCriteria();
                        for (String s : completedIter) {
                            completedSet.add(s);
                        }
                        
                        foundInGame = true;

                        // Use server holder for criteria map
                        Map<String, ?> criteriaMap = serverHolder.value().criteria();
                        if (criteriaMap != null && !criteriaMap.isEmpty()) {
                             for (String c : criteriaMap.keySet()) {
                                 boolean isCompleted = completedSet.contains(c);
                                 if (!isCompleted) {
                                     if (c.contains(":")) {
                                         String path = c.split(":")[1];
                                         isCompleted = completedSet.contains(path);
                                     } else {
                                         isCompleted = completedSet.contains("minecraft:" + c);
                                     }
                                 }
                                 result.add(new CriteriaInfo(c, getCriterionName(c), isCompleted));
                             }
                             
                             result.sort(Comparator.comparing(info -> info.displayName.getString()));
                             return result;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (progressField != null) {
                @SuppressWarnings("unchecked")
                Map<AdvancementHolder, AdvancementProgress> progressMap = 
                    (Map<AdvancementHolder, AdvancementProgress>) progressField.get(advancements);
                
                if (progressMap != null) {
                     boolean containsKey = false;
                     for (AdvancementHolder h : progressMap.keySet()) {
                         if (h.id().equals(targetId)) {
                             containsKey = true;
                             break;
                         }
                     }
                     
                     if (!containsKey) {
                         // Force re-request if not found (throttle to once per second)
                         if (System.currentTimeMillis() % 1000 < 50) {
                             requestUpdate(advancementId);
                         }
                     }
                }

                if (progressMap != null) {
                    for (Map.Entry<AdvancementHolder, AdvancementProgress> entry : progressMap.entrySet()) {
                        AdvancementHolder holder = entry.getKey();
                        AdvancementProgress prog = entry.getValue();

                        if (holder.id().equals(targetId)) {
                            foundInGame = true;
                            // Get completed criteria from progress
                            Iterable<String> completedIter = prog.getCompletedCriteria();
                            for (String s : completedIter) {
                                completedSet.add(s);
                            }

                            // Always prefer game criteria (Source of Truth) over hardcoded definitions
                            // This ensures we match exactly what the game expects (including modded biomes/items)
                            Map<String, ?> criteriaMap = holder.value().criteria();
                            
                            if (criteriaMap != null && !criteriaMap.isEmpty()) {
                                Set<String> processedCriteria = new HashSet<>();
                                
                                for (String c : criteriaMap.keySet()) {
                                    // Filter out technical criteria if needed (usually not for collection advancements)
                                    boolean isCompleted = completedSet.contains(c);

                                    // Fuzzy matching for namespaces (fixes Adventuring Time and others)
                                    if (!isCompleted) {
                                        if (c.contains(":")) {
                                            // Try without namespace (e.g. "minecraft:plains" -> "plains")
                                            String path = c.substring(c.indexOf(":") + 1);
                                            isCompleted = completedSet.contains(path);
                                        } else {
                                            // Try with minecraft namespace (e.g. "plains" -> "minecraft:plains")
                                            isCompleted = completedSet.contains("minecraft:" + c);
                                        }
                                    }
                                    
                                    // Special fallback for A Complete Catalogue if keys mismatch
                                    if (!isCompleted && advancementId.contains("complete_catalogue")) {
                                         // Sometimes keys might be stripped or formatted differently?
                                         // Just in case, try contains match
                                         for (String completed : completedSet) {
                                             if (completed.endsWith(c) || c.endsWith(completed)) {
                                                 isCompleted = true;
                                                 break;
                                             }
                                         }
                                    }

                                    // DEBUG: Log for Adventuring Time specifically if still not completed
                                    if (!isCompleted && advancementId.contains("adventuring_time")) {
                                        if (System.currentTimeMillis() % 10000 < 50) {
                                             // System.out.println("DEBUG: Adventuring Time missing: " + c + " (Have: " + completedSet + ")");
                                        }
                                    }
                                    
                                    result.add(new CriteriaInfo(c, getCriterionName(c), isCompleted));
                                    processedCriteria.add(c);
                                }
                                
                                result.sort(Comparator.comparing(info -> info.displayName.getString()));
                                return result;
                            }
                            break;
                        }
                    }
                }
            } else {
                 // System.err.println("SPEEDRUN_MOD: progressField is null, cannot read progress!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Fallback: If we didn't find it in game or it had no criteria, use hardcoded definitions
        if (AdvancementDefinitions.DEFINITIONS.containsKey(advancementId)) {
            List<String> definedCriteria = AdvancementDefinitions.DEFINITIONS.get(advancementId);
            for (String c : definedCriteria) {
                // Check exact match or namespace-less match
                boolean isCompleted = completedSet.contains(c);
                if (!isCompleted && c.contains(":")) {
                    String path = c.split(":")[1];
                    isCompleted = completedSet.contains(path);
                }
                if (!isCompleted && !c.contains(":")) {
                     isCompleted = completedSet.contains("minecraft:" + c);
                }
                
                result.add(new CriteriaInfo(c, getCriterionName(c), isCompleted));
            }
        }
        
        // Sort by display name
        result.sort(Comparator.comparing(info -> info.displayName.getString()));
        return result;
    }

    private static String formatName(String path) {
        if (path == null) return "";
        String name = path.replace("_", " ");
        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name;
    }

    private static Component getCriterionName(String criterion) {
        // Special handling for textures (cats)
        if (criterion.contains("textures/entity/cat/")) {
            String name = criterion.substring(criterion.lastIndexOf("/") + 1).replace(".png", "");
            // Capitalize
            if (!name.isEmpty()) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
            }
            return Component.literal(name);
        }

        // Many criteria use resource locations as names (e.g. minecraft:apple)
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(criterion);
        if (id != null) {
            // Try Item
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                return BuiltInRegistries.ITEM.get(id).map(ref -> new net.minecraft.world.item.ItemStack(ref.value()).getHoverName()).orElse(Component.literal(id.toString()));
            }
            // Try Entity
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return BuiltInRegistries.ENTITY_TYPE.get(id).map(ref -> ref.value().getDescription()).orElse(Component.literal(id.toString()));
            }
            // Try Cat Variant (1.21+)
            try {
                // Using reflection or dynamic lookup if CAT_VARIANT is not statically accessible in this mapping
                // But it should be in BuiltInRegistries
                // Check if we can access it via generic registry lookup first to be safe
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    var registryEntry = mc.level.registryAccess().registries()
                            .filter(r -> r.key().toString().contains("cat_variant"))
                            .findFirst();
                    if (registryEntry.isPresent()) {
                         net.minecraft.core.Registry<?> catRegistry = registryEntry.get().value();
                         if (catRegistry.containsKey(id)) {
                             String key = "cat_variant." + id.getNamespace() + "." + id.getPath(); // Standard translation key format?
                             // Actually, cat variants usually don't have direct translation keys in lang file like this?
                             // Usually it's entity.minecraft.cat.tabby ? No.
                             // Let's try to format it nicely if no translation found.
                             return Component.translatable(key).getString().equals(key) ? 
                                    Component.literal(formatName(id.getPath())) : 
                                    Component.translatable(key);
                         }
                    }
                }
            } catch (Exception e) {}

            // Try Biome (Use dynamic registry via stream to avoid method signature issues)
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                 try {
                     var registryEntry = mc.level.registryAccess().registries()
                             .filter(r -> r.key().equals(net.minecraft.core.registries.Registries.BIOME))
                             .findFirst();
                     
                     if (registryEntry.isPresent()) {
                         net.minecraft.core.Registry<?> biomeRegistry = registryEntry.get().value();
                         if (biomeRegistry.containsKey(id)) {
                              String key = "biome." + id.getNamespace() + "." + id.getPath();
                              return Component.translatable(key);
                         }
                     }
                 } catch (Exception e) {
                     // Ignore
                 }
            }
        }
        
        // Fallback: just return the criterion string, maybe simplified
        return Component.literal(criterion);
    }

    // private static boolean isBiome(String criterion) {
    //    // Deprecated: We use BuiltInRegistries.BIOME now
    //    return false;
    // }
}
