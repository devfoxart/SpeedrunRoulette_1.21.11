package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.world.level.storage.LevelResource;
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

    public static String pendingVictoryTime = null;
    public static String pendingVictoryObjectiveName = null;
    
    // Auto-open wheel state
    public static boolean hasCheckedAutoOpen = false;

    public SpeedrunRoulette(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::registerGuiLayers);
        
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(new SpeedrunClientEvents());
        NeoForge.EVENT_BUS.register(new SpeedrunServerEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
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
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !SpeedrunRoulette.hasCheckedAutoOpen) {
                SpeedrunRoulette.hasCheckedAutoOpen = true;
                SpeedrunState.checkAutoOpen();
            }

            if (mc.isPaused()) {
                SpeedrunState.onSystemPause(true);
            } else {
                SpeedrunState.onSystemPause(false);
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
        }

        @SubscribeEvent
        public void onScreenClosing(ScreenEvent.Closing event) {
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
                        SpeedrunState.setObjectives(saved, false);
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
            
            boolean isVictory = SpeedrunRoulette.pendingVictoryTime != null;
            boolean isActive = SpeedrunState.hasActiveObjectives();

            if (server.isSingleplayer() && (isVictory || (isActive && !SpeedrunState.isCompleted()))) {
                try {
                     Path worldDir = server.getWorldPath(LevelResource.ROOT);
                     Path statusFile = worldDir.resolve("speedrun_status.txt");
                     
                     String status = isVictory ? "SUCCESS" : "FAILURE";
                     String time = isVictory ? SpeedrunRoulette.pendingVictoryTime : "";
                     
                     String objName = "";
                     if (SpeedrunRoulette.pendingVictoryObjectiveName != null) {
                         objName = SpeedrunRoulette.pendingVictoryObjectiveName;
                     } else {
                         List<Objective> objs = SpeedrunState.getObjectives();
                         if (objs != null && !objs.isEmpty()) {
                             if (objs.size() > 1) {
                                 objName = "Liste de " + objs.size() + " items";
                             } else {
                                 objName = objs.get(0).getDisplayName().getString();
                             }
                         } else {
                             objName = "Speedrun";
                         }
                     }
                     
                     String content = status + "\n" + objName + "\n" + time;
                     Files.writeString(statusFile, content);
                     
                     // Reset victory state
                     SpeedrunRoulette.pendingVictoryTime = null;
                     SpeedrunRoulette.pendingVictoryObjectiveName = null;
                     
                } catch (Exception e) {
                    LOGGER.error("ServerStopping: Error writing speedrun status", e);
                }
            }
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
