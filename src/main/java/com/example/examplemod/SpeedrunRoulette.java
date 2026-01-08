package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;

import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.minecraft.resources.Identifier;

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

    public static boolean pendingGiveUp = false;
    public static boolean pendingReplay = false;
    public static boolean pendingNewRun = false;
    
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
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // Create the Category object.
        KeyMapping.Category category = new KeyMapping.Category(Identifier.tryParse("examplemod:speedrun_roulette"));
        
        event.registerCategory(category);
        
        OPEN_WHEEL_KEY = new KeyMapping("key.examplemod.open_wheel", InputConstants.Type.KEYSYM, InputConstants.KEY_R, category);
        event.register(OPEN_WHEEL_KEY);

        PAUSE_TIMER_KEY = new KeyMapping("key.examplemod.pause_timer", InputConstants.Type.KEYSYM, InputConstants.KEY_P, category);
        event.register(PAUSE_TIMER_KEY);

        TOGGLE_HUD_KEY = new KeyMapping("key.examplemod.toggle_hud", InputConstants.Type.KEYSYM, InputConstants.KEY_H, category);
        event.register(TOGGLE_HUD_KEY);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.tryParse(SpeedrunRoulette.MODID + ":hud"), (guiGraphics, partialTick) -> {
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
            if (mc.isPaused()) {
                SpeedrunState.onSystemPause(true);
            } else {
                // Only resume system pause if we were system paused.
                // We rely on SpeedrunState to handle "isManualPaused" separately.
                SpeedrunState.onSystemPause(false);
            }

            // Folder renaming logic removed as per request to prevent infinite loading.
            // Renaming is now handled by setting LevelName in onServerStopping.


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

        @SubscribeEvent
        public void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
            if (event.getScreen() instanceof PoolCustomizationScreen screen) {
                if (screen.handleMouseClick(event.getMouseX(), event.getMouseY(), event.getButton())) {
                    event.setCanceled(true);
                }
            }
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
                        // Also ensure timer is started if not running?
                        // User didn't specify, but usually yes.
                        // But wait, if we reload the world, we lose the 'startTime'.
                        // We should probably save the timer state too if we want true persistence.
                        // But user only asked for "World Locking" (objectives fixed).
                        // "le monde est "bloqué" avec cette item le roue ne peut pas etre retoruner".
                        // It doesn't explicitly say "Timer must resume exactly where left off".
                        // But "renommé nom de l'objetif + echec" implies tracking.
                        // If we reload, and timer is 0, it's a fresh run with SAME objectives.
                        // That seems consistent with "World Locking".
                        // So we just set objectives.
                    } else {
                        // Clear objectives for new world (or world with no saved objectives)
                        SpeedrunState.clearObjectives();
                    }
                }
            }
        }

        @SubscribeEvent
        public void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
            SpeedrunRoulette.hasCheckedAutoOpen = false;
            SpeedrunState.markObjectivesStale();
        }

        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {
            net.minecraft.server.MinecraftServer server = event.getServer();
            
            // 1. Handle Level Renaming (In-World Notes)
            boolean isVictory = SpeedrunRoulette.pendingVictoryTime != null;
            boolean isGiveUp = SpeedrunRoulette.pendingGiveUp;
            boolean isActive = SpeedrunState.hasActiveObjectives();

            // We rename if it's a Victory OR if we are Giving Up (and have active objectives)
            // If just quitting to menu without GiveUp/Victory, we don't rename (keep existing name).
            if (server.isSingleplayer() && (isVictory || (isGiveUp && isActive))) {
                try {
                    net.minecraft.world.level.storage.WorldData wd = server.getWorldData();
                    String currentDisplayName = wd.getLevelName();

                    if (!currentDisplayName.contains("Echec") && !currentDisplayName.contains("Success") && !currentDisplayName.contains(" - ")) {
                        
                        String objPrefix = "";
                        // Use pending name if set, or calculate from objectives
                        if (SpeedrunRoulette.pendingVictoryObjectiveName != null) {
                            objPrefix = SpeedrunRoulette.pendingVictoryObjectiveName;
                        } else {
                            List<Objective> objs = SpeedrunState.getObjectives();
                            if (objs != null && !objs.isEmpty()) {
                                if (objs.size() > 1) {
                                    objPrefix = "Liste de " + objs.size() + " items";
                                } else {
                                    objPrefix = objs.get(0).getDisplayName().getString();
                                }
                            } else {
                                objPrefix = "Speedrun";
                            }
                        }
                        
                        String suffix;
                        if (isVictory) {
                            String safeTime = SpeedrunRoulette.pendingVictoryTime != null ? SpeedrunRoulette.pendingVictoryTime.replace(":", " ") : "00 00";
                            suffix = " - " + safeTime;
                        } else {
                            suffix = " - Echec";
                        }
                        
                        String newDisplayName = objPrefix + suffix;
                        
                        LOGGER.info("ServerStopping: Updating Level Name to: " + newDisplayName);
                        try {
                            java.lang.reflect.Method m = wd.getClass().getMethod("setLevelName", String.class);
                            m.invoke(wd, newDisplayName);
                        } catch (Exception e) {
                            LOGGER.error("Failed to set level name via reflection", e);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("ServerStopping: Error getting world name", e);
                }
            }

            // 2. Handle State Transitions (Prepare for Next Run)
            // We do this AFTER renaming so that objectives are still available for renaming logic.
            if (SpeedrunRoulette.pendingGiveUp || SpeedrunRoulette.pendingNewRun) {
                LOGGER.info("ServerStopping: Preparing for New Game (Clear Objectives)");
                SpeedrunState.prepareForNewGame();
            } else if (SpeedrunRoulette.pendingReplay) {
                LOGGER.info("ServerStopping: Preparing for Retry (Keep Objectives)");
                SpeedrunState.prepareForRetry();
            }
            
            // Reset Flags
            SpeedrunRoulette.pendingGiveUp = false;
            SpeedrunRoulette.pendingNewRun = false;
            SpeedrunRoulette.pendingReplay = false;
            SpeedrunRoulette.pendingVictoryTime = null;
            SpeedrunRoulette.pendingVictoryObjectiveName = null;
            SpeedrunRoulette.pendingLevelNewName = null;
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
