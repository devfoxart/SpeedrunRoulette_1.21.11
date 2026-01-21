package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;

import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
// import net.minecraft.resources.ResourceLocation;

@Mod(SpeedrunRoulette.MODID)
public class SpeedrunRoulette {
    public static final String MODID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static KeyMapping OPEN_WHEEL_KEY;
    public static KeyMapping PAUSE_TIMER_KEY;
    public static KeyMapping TOGGLE_HUD_KEY;

    public static String pendingLevelRenameId = null;
    public static String pendingLevelNewName = null;
    public static String pendingVictoryTime = null;
    public static String pendingVictoryObjectiveName = null;
    
    // Auto-open wheel state
    public static boolean hasCheckedAutoOpen = false;

    public SpeedrunRoulette(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::registerGuiLayers);
        
        // BLOCKS.register(modEventBus);
        // ITEMS.register(modEventBus);
        // CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(new SpeedrunClientEvents());
        NeoForge.EVENT_BUS.register(new SpeedrunServerEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Common setup logic if needed
        // LOGGER.info("DEBUG: Common Setup - Checking hardcoded advancements");
        
        // DEBUG: Check candidates on common setup (tests hardcoded fallback)
        // List<Objective> advancements = ObjectivePoolHelper.getAllCandidates(true, false, false, true);
        // LOGGER.info("DEBUG: Found {} advancements in hardcoded pool", advancements.size());
        // for (int i = 0; i < Math.min(advancements.size(), 10); i++) {
        //    LOGGER.info("DEBUG: Hardcoded Advancement candidate: {}", advancements.get(i).getId());
        // }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // Use Identifier for category
        // Identifier constructor is private, use tryParse (or of/new if public in this version, but tryParse is safest if we check null)
        net.minecraft.resources.Identifier categoryId = net.minecraft.resources.Identifier.tryParse("examplemod:general");
        if (categoryId == null) {
             // Fallback if parsing failed (shouldn't happen)
             // But tryParse returns null on invalid chars. "examplemod:general" is valid.
             // If we really need a fallback, we might be stuck.
             // Let's assume it works.
             // If we must have a non-null, maybe use a dummy or existing one.
        }
        
        KeyMapping.Category category = new KeyMapping.Category(categoryId != null ? categoryId : net.minecraft.resources.Identifier.tryParse("minecraft:misc"));
        
        OPEN_WHEEL_KEY = new KeyMapping("key.examplemod.open_wheel", com.mojang.blaze3d.platform.InputConstants.KEY_R, category);
        event.register(OPEN_WHEEL_KEY);

        PAUSE_TIMER_KEY = new KeyMapping("key.examplemod.pause_timer", com.mojang.blaze3d.platform.InputConstants.KEY_P, category);
        event.register(PAUSE_TIMER_KEY);

        TOGGLE_HUD_KEY = new KeyMapping("key.examplemod.toggle_hud", com.mojang.blaze3d.platform.InputConstants.KEY_H, category);
        event.register(TOGGLE_HUD_KEY);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(net.minecraft.resources.Identifier.tryParse(SpeedrunRoulette.MODID + ":hud"), (guiGraphics, partialTick) -> {
            SpeedrunState.onRenderHud(guiGraphics);
        });
    }

    public static class SpeedrunClientEvents {
        @SubscribeEvent
        public void onClientTick(ClientTickEvent.Post event) {
            // Auto-open wheel logic
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !SpeedrunRoulette.hasCheckedAutoOpen) {
                SpeedrunRoulette.hasCheckedAutoOpen = true;
                SpeedrunState.checkAutoOpen();
            }

            // Sync system pause state with Minecraft's pause state
            // This handles ALL menus (Options, Controls, Advancements, etc.) automatically.
            // If the game is paused (singleplayer menu open), we pause the timer.
            // Also explicitly pause if ReminderScreen is open (even if game not paused, e.g. MP)
            if (mc.isPaused() || (mc.screen instanceof ReminderScreen)) {
                SpeedrunState.onSystemPause(true);
            } else {
                // Only resume system pause if we were system paused.
                // We rely on SpeedrunState to handle "isManualPaused" separately.
                SpeedrunState.onSystemPause(false);
            }

            if (SpeedrunRoulette.pendingLevelRenameId != null) {
                if (mc.getSingleplayerServer() == null) {
                    String levelId = SpeedrunRoulette.pendingLevelRenameId;
                    String newName = SpeedrunRoulette.pendingLevelNewName;
                    SpeedrunRoulette.pendingLevelRenameId = null; 
                    SpeedrunRoulette.pendingLevelNewName = null;
                    
                    // LOGGER.info("ClientTick: Server stopped. Renaming level " + levelId + " to " + newName);
                    
                    try {
                        net.minecraft.world.level.storage.LevelStorageSource source = mc.getLevelSource();
                        if (source.levelExists(levelId)) {
                            try (net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess access = source.createAccess(levelId)) {
                                access.renameLevel(newName);
                                // LOGGER.info("ClientTick: Level renamed successfully via LevelStorageAccess!");
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("ClientTick: Failed to rename level: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            if (OPEN_WHEEL_KEY != null && OPEN_WHEEL_KEY.consumeClick()) {
                SpeedrunState.openWheelOrReminder();
            }
            if (PAUSE_TIMER_KEY != null && PAUSE_TIMER_KEY.consumeClick()) {
                SpeedrunState.toggleManualPause();
            }
            if (TOGGLE_HUD_KEY != null && TOGGLE_HUD_KEY.consumeClick()) {
                SpeedrunState.toggleHud();
            }

            SpeedrunState.onClientTick();
        }

        @SubscribeEvent
        public void onScreenInit(ScreenEvent.Init.Post event) {
            SpeedrunState.onScreenInit(event);
            // We rely on ClientTick to handle pause state now.
        }

        @SubscribeEvent
        public void onScreenClosing(ScreenEvent.Closing event) {
             // We rely on ClientTick to handle pause state now.
        }

        private record CachedWorldData(List<Component> tooltip, int status) {}
        private static final Map<String, CachedWorldData> worldTooltipCache = new HashMap<>();

        @SubscribeEvent
        public void onScreenRender(ScreenEvent.Render.Post event) {
            if (event.getScreen() instanceof SelectWorldScreen screen) {
                 // LOGGER.info("DEBUG: Rendering SelectWorldScreen");
                 Minecraft mc = Minecraft.getInstance();
                int mouseX = event.getMouseX();
                int mouseY = event.getMouseY();
                
                WorldSelectionList list = null;
                for (net.minecraft.client.gui.components.events.GuiEventListener child : screen.children()) {
                    if (child instanceof WorldSelectionList l) {
                        list = l;
                        break;
                    }
                }
                
                if (list != null) {
                    // LOGGER.info("DEBUG: Found WorldSelectionList with " + list.children().size() + " children");
                    try {
                        // Get itemHeight via reflection (it's protected in AbstractSelectionList)
                        java.lang.reflect.Field itemHeightField = null;
                        Class<?> clazz = list.getClass();
                        while (clazz != null && itemHeightField == null) {
                            try {
                                itemHeightField = clazz.getDeclaredField("itemHeight");
                            } catch (NoSuchFieldException e) {
                                clazz = clazz.getSuperclass();
                            }
                        }
                        
                        int itemHeight = 36;
                        if (itemHeightField != null) {
                            itemHeightField.setAccessible(true);
                            itemHeight = itemHeightField.getInt(list);
                        } else {
                            // LOGGER.warn("DEBUG: Could not find itemHeight field, using default 36");
                        }
                        
                        // Proceed even if reflection failed (using default itemHeight)
                        {
                            // Get other properties
                            int listTop = list.getY(); 
                            int listHeight = list.getHeight();
                            int listBottom = listTop + listHeight;
                            int listLeft = list.getX();
                            int listWidth = list.getWidth();
                            
                            // Get scroll via reflection
                            double scroll = 0;
                            try {
                                java.lang.reflect.Method m = list.getClass().getMethod("getScrollAmount");
                                scroll = (double) m.invoke(list);
                            } catch (Exception e) {
                                Class<?> c = list.getClass();
                                while (c != null) {
                                    try {
                                        java.lang.reflect.Field f = c.getDeclaredField("scrollAmount");
                                        f.setAccessible(true);
                                        scroll = f.getDouble(list);
                                        break;
                                    } catch (NoSuchFieldException ex) {
                                        c = c.getSuperclass();
                                    }
                                }
                            }
                            
                            // Get headerHeight via reflection
                            int headerHeight = 0;
                            try {
                                java.lang.reflect.Method m = list.getClass().getMethod("getHeaderHeight");
                                headerHeight = (int) m.invoke(list);
                            } catch (Exception e) {
                                Class<?> c = list.getClass();
                                while (c != null) {
                                    try {
                                        java.lang.reflect.Field f = c.getDeclaredField("headerHeight");
                                        f.setAccessible(true);
                                        headerHeight = f.getInt(list);
                                        break;
                                    } catch (NoSuchFieldException ex) {
                                        c = c.getSuperclass();
                                    }
                                }
                            }
                            
                            java.util.List<WorldSelectionList.Entry> children = (java.util.List<WorldSelectionList.Entry>) list.children();
                            
                            for (int i = 0; i < children.size(); i++) {
                                WorldSelectionList.Entry entry = children.get(i);
                                
                                int rowTop = listTop + 4 - (int)scroll + (i * itemHeight) + headerHeight;
                                int rowBottom = rowTop + itemHeight;
                                
                                // Only process if visible
                                if (rowBottom >= listTop && rowTop <= listBottom) {
                                    // LOGGER.info("DEBUG: Processing entry " + i + " class: " + entry.getClass().getName());
                                    // Check if this entry has speedrun data
                                    if (entry.getClass().getName().contains("WorldListEntry")) {
                                        try {
                                            java.lang.reflect.Field summaryField = entry.getClass().getDeclaredField("summary");
                                            summaryField.setAccessible(true);
                                            LevelSummary summary = (LevelSummary) summaryField.get(entry);
                                            String levelId = summary.getLevelId();
                                            
                                            // Load/Check cache
                                            CachedWorldData cachedData = worldTooltipCache.get(levelId);
                                            if (cachedData == null) {
                                                cachedData = loadWorldData(mc, levelId);
                                                worldTooltipCache.put(levelId, cachedData);
                                            }
                                            
                                            List<Component> tooltip = cachedData.tooltip();
                                            int status = cachedData.status(); // 0=None/InProgress, 1=Won, 2=Lost
                                            
                                            boolean hasData = tooltip != null && !tooltip.isEmpty() && !tooltip.get(0).equals(Component.empty());
                                            
                                            // LOGGER.info("DEBUG: Entry " + levelId + " status=" + status + " hasData=" + hasData + " tooltipSize=" + (tooltip != null ? tooltip.size() : "null"));
                                            
                                            if (hasData) {
                                                // Render Icon
                                                int rowWidth = list.getRowWidth();
                                                int rowLeft = listLeft + (listWidth - rowWidth) / 2;
                                                // Adjust Icon X position - Move it more to the right or left to be visible
                                                int iconX = rowLeft + rowWidth - 24; // Right side inside the row
                                                int iconY = rowTop + (itemHeight - 16) / 2;
                                                
                                                // LOGGER.info("DEBUG: Rendering icon for level " + levelId + " at " + iconX + "," + iconY + " status=" + status);
                                                
                                                // Ensure we render on top
                                                // com.mojang.blaze3d.vertex.PoseStack pose = event.getGuiGraphics().pose();
                                                // pose.pushPose();
                                                // pose.translate(0.0F, 0.0F, 100.0F); // Bring forward

                                                // Scissor to clip icons at list edges
                                                event.getGuiGraphics().enableScissor(listLeft, listTop, listLeft + listWidth, listBottom);

                                                if (status == 1) {
                                                    // Won - Green Checkmark (✔) - 0xFF55FF55 (Opaque Light Green)
                                                    event.getGuiGraphics().drawString(mc.font, "✔", iconX + 4, iconY + 4, 0xFF55FF55, true);
                                                } else if (status == 2) {
                                                    // Lost - Red Cross (✘) - 0xFFFF5555 (Opaque Light Red)
                                                    event.getGuiGraphics().drawString(mc.font, "✘", iconX + 4, iconY + 4, 0xFFFF5555, true);
                                                }
                                                
                                                event.getGuiGraphics().disableScissor();
                                                
                                                // pose.popPose();
                                                
                                                // Check Hover for Tooltip
                                                // User requested: Icon must be clipped (handled by enableScissor above), 
                                                // but Tooltip must show AS SOON AS the icon is hovered, even if partially visible.
                                                // Removed margins to ensure tooltip triggers on all visible parts of the icon.
                                                if (mouseX >= rowLeft && mouseX <= rowLeft + rowWidth && 
                                                    mouseY >= rowTop && mouseY <= rowBottom &&
                                                    mouseY >= listTop && mouseY <= listBottom) { // Strict scissor check (no extra margin)
                                                    
                                                    // Check if hovering icon specifically or the row? 
                                                    // User said "toggle info a coter des monde" - maybe they want hover on icon?
                                                    // The original code checked the whole row, let's keep it but maybe restrict to icon if it conflicts?
                                                    // For now, let's just make sure it renders.
                                                    
                                                    // Check if we are hovering the icon area specifically to avoid spamming tooltip on whole row
                                                    // But original requirement was likely just hover row or icon.
                                                    // Let's refine to Icon area to be safe and avoid conflict with selection.
                                                    if (mouseX >= iconX && mouseX <= iconX + 16 && mouseY >= iconY && mouseY <= iconY + 16) {
                                                       renderTooltip(event.getGuiGraphics(), mc.font, tooltip, mouseX, mouseY, screen.width, screen.height);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Ignore entry specific errors
                                            // LOGGER.error("DEBUG: Entry error: " + e.getMessage());
                                            // e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            
                            // LOGGER.info("DEBUG: List dimensions: top=" + listTop + " bottom=" + listBottom + " itemHeight=" + itemHeight + " scroll=" + scroll);
                        }
                    } catch (Exception e) {
                        // LOGGER.error("DEBUG: General rendering error", e);
                    }
                }
            } else {
                if (!worldTooltipCache.isEmpty()) {
                    worldTooltipCache.clear();
                }
            }
        }
        
        private CachedWorldData loadWorldData(Minecraft mc, String levelId) {
             java.util.List<Component> tooltip = new ArrayList<>();
             int status = 0; // 0=None, 1=Won, 2=Lost
             
             try {
                 Path saveDir = mc.getLevelSource().getBaseDir();
                 Path levelDir = saveDir.resolve(levelId);
                 Path dataFile = levelDir.resolve("data").resolve("speedrun_world_data.dat");
                 
                 if (Files.exists(dataFile)) {
                     CompoundTag tag = NbtIo.readCompressed(dataFile, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                     if (tag != null) {
                         CompoundTag data = new CompoundTag();
                         try {
                             if (tag.contains("data")) {
                                 Object dataObj = tag.get("data");
                                 if (dataObj instanceof CompoundTag) {
                                     data = (CompoundTag) dataObj;
                                 } else if (dataObj instanceof java.util.Optional) {
                                     data = ((java.util.Optional<CompoundTag>) dataObj).orElse(new CompoundTag());
                                 }
                             }
                         } catch (Throwable t) {
                              if (tag.contains("data")) {
                                  // fallback
                              }
                         }
                        
                         // Read Status
                         boolean isWon = false;
                         boolean isLost = false;
                         long totalTime = 0;
                         
                         // Helper for reading boolean/long safely
                         try { isWon = data.getBoolean("isWon").orElse(false); } catch(Throwable t) {}
                         try { isLost = data.getBoolean("isLost").orElse(false); } catch(Throwable t) {}
                         try { totalTime = data.getLong("totalTime").orElse(0L); } catch(Throwable t) {}

                         if (isWon) {
                             status = 1;
                             tooltip.add(Component.translatable("gui.examplemod.tooltip.speedrun_won").withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
                             tooltip.add(Component.translatable("gui.examplemod.tooltip.time", formatTime(totalTime)).withStyle(net.minecraft.ChatFormatting.GRAY));
                         } else {
                             // Treat In-Progress or Lost as Failure
                             status = 2;
                             tooltip.add(Component.translatable("gui.examplemod.tooltip.speedrun_failed").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
                         }
                         
                         // Read Objectives
                         if (data.contains("objectives")) {
                             net.minecraft.nbt.ListTag list = null;
                             try {
                                 Object listObj = data.getList("objectives");
                                 if (listObj instanceof java.util.Optional) {
                                     list = ((java.util.Optional<net.minecraft.nbt.ListTag>) listObj).orElse(new net.minecraft.nbt.ListTag());
                                 } else {
                                     list = (net.minecraft.nbt.ListTag) listObj;
                                 }
                             } catch(Throwable t) {}

                             if (list != null && !list.isEmpty()) {
                                 tooltip.add(Component.empty());
                                 tooltip.add(Component.translatable("gui.examplemod.tooltip.objectives").withStyle(net.minecraft.ChatFormatting.WHITE));
                                 
                                 for (int i = 0; i < list.size(); i++) {
                                     CompoundTag objTag = list.getCompound(i).orElse(new CompoundTag());
                                     
                                     // DEBUG: Log the tag content to understand why name is missing
                                     // LOGGER.info("DEBUG: Objective Tag: " + objTag.toString());
                                     
                                     Component nameComp = null;
                                     String nameJson = objTag.getString("displayName").orElse("");
                                     
                                     if (!nameJson.isEmpty()) {
                                          try {
                                              JsonElement element = JsonParser.parseString(nameJson);
                                              nameComp = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element).result().orElse(null);
                                          } catch (Exception e) {
                                              // Try literal if parsing fails
                                              nameComp = Component.literal(nameJson);
                                          }
                                     }
                                     
                                     if (nameComp == null) {
                                          // Fallback to description
                                          String descJson = objTag.getString("description").orElse("");
                                          if (!descJson.isEmpty()) {
                                              try {
                                                  JsonElement element = JsonParser.parseString(descJson);
                                                  nameComp = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element).result().orElse(null);
                                              } catch (Exception e) {
                                                  nameComp = Component.literal(descJson);
                                              }
                                          }
                                     }
                                     
                                     // Final fallback: Use ID or Type
                                     if (nameComp == null) {
                                         String id = objTag.getString("id").orElse("Unknown");
                                         // Try to make ID readable
                                         if (id.contains(":")) id = id.split(":")[1];
                                         id = id.replace('_', ' ');
                                         // Capitalize
                                         if (id.length() > 0) {
                                             id = Character.toUpperCase(id.charAt(0)) + id.substring(1);
                                         }
                                         nameComp = Component.literal(id);
                                     }
                                     
                                     // Use forceCompleted because Objective.save() stores it as such
                                     boolean completed = objTag.getBoolean("forceCompleted").orElse(false);
                                     
                                     if (completed) {
                                         tooltip.add(Component.literal("§a✔ ").append(nameComp));
                                     } else {
                                         tooltip.add(Component.literal("§7- ").append(nameComp));
                                     }
                                 }
                             }
                         }
                     }
                 }
             } catch (Exception e) {
                 LOGGER.error("Failed to load world data for tooltip", e);
             }
             
             return new CachedWorldData(tooltip, status);
        }
        
        private String formatTime(long nanos) {
            long millis = nanos / 1_000_000L;
            long seconds = millis / 1000L;
            long minutes = seconds / 60L;
            long hours = minutes / 60L;
            
            if (hours > 0) {
                return String.format("%d:%02d:%02d.%03d", hours, minutes % 60, seconds % 60, millis % 1000);
            } else {
                return String.format("%02d:%02d.%03d", minutes % 60, seconds % 60, millis % 1000);
            }
        }

        private void renderTooltip(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, java.util.List<Component> text, int x, int y, int screenWidth, int screenHeight) {
            if (text.isEmpty()) return;
            
            // guiGraphics.disableScissor();
            
            int maxWidth = 0;
            for (Component c : text) {
                int w = font.width(c);
                if (w > maxWidth) maxWidth = w;
            }
            
            int lineHeight = 10;
            int totalHeight = text.size() * lineHeight + 6;
            int totalWidth = maxWidth + 8;
            
            int bx = x + 10;
            int by = y - 5;
            
            if (bx + totalWidth > screenWidth) bx -= (totalWidth + 20);
            if (by + totalHeight > screenHeight) by -= totalHeight;
            
            guiGraphics.fill(bx, by, bx + totalWidth, by + totalHeight, 0xF0100010);
            guiGraphics.renderOutline(bx, by, totalWidth, totalHeight, 0x505000FF); 
            
            for (int i = 0; i < text.size(); i++) {
                guiGraphics.drawString(font, text.get(i), bx + 4, by + 4 + (i * lineHeight), 0xFFFFFFFF, false);
            }
            
            // Depth test handled by GuiGraphics
        }
    }

    public static class SpeedrunServerEvents {
        @SubscribeEvent
        public void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                net.minecraft.server.MinecraftServer server = null;
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                     server = serverLevel.getServer();
                }
                
                if (server != null && server.isSingleplayer()) {
                    SpeedrunWorldData data = SpeedrunWorldData.get(server);
                    List<Objective> saved = data.getObjectives();
                    if (!saved.isEmpty()) {
                        // Direct sync for Singleplayer
                        SpeedrunState.setObjectives(saved, false);
                    }
                }
            }
        }

        @SubscribeEvent
        public void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
            SpeedrunRoulette.hasCheckedAutoOpen = false;
            SpeedrunState.markObjectivesStale();
            SpeedrunState.resetTimer();
        }

        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {
            // World renaming disabled by user request
        }


        @SubscribeEvent
        public void onAdvancementProgress(AdvancementEvent.AdvancementProgressEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                List<Objective> objs = SpeedrunState.getObjectives();
                if (objs != null) {
                    for (Objective obj : objs) {
                        if (obj.getType() == Objective.Type.ADVANCEMENT) {
                            String advId = obj.getAdvancementId();
                            if (advId != null && advId.equals(event.getAdvancement().id().toString())) {
                                if (event.getAdvancementProgress().isDone()) {
                                    LOGGER.info("Advancement completed: " + advId);
                                    obj.setForceCompleted(true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}