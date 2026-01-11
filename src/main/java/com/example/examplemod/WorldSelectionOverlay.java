package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = SpeedrunRoulette.MODID, value = Dist.CLIENT)
public class WorldSelectionOverlay {

    private static final Map<String, RunStatus> STATUS_CACHE = new HashMap<>();
    private static Field summaryField;
    private static Field itemHeightField;
    private static Method getScrollAmountMethod;
    private static Field scrollAmountField;

    static {
        try {
            // Reflection setup
            Class<?> entryClass = WorldSelectionList.WorldListEntry.class;
            for (Field f : entryClass.getDeclaredFields()) {
                if (f.getType() == LevelSummary.class) {
                    summaryField = f;
                    summaryField.setAccessible(true);
                    SpeedrunRoulette.LOGGER.info("WorldSelectionOverlay: Found summary field: " + f.getName());
                    break;
                }
            }
            
            // Access itemHeight and scrollAmount from AbstractSelectionList
            Class<?> listClass = net.minecraft.client.gui.components.AbstractSelectionList.class;
            
            // 1. Find itemHeight (mapped as defaultEntryHeight in some versions)
            try {
                 itemHeightField = listClass.getDeclaredField("itemHeight");
                 itemHeightField.setAccessible(true);
                 SpeedrunRoulette.LOGGER.info("WorldSelectionOverlay: Found itemHeight field");
            } catch (NoSuchFieldException e) {
                 try {
                     itemHeightField = listClass.getDeclaredField("defaultEntryHeight");
                     itemHeightField.setAccessible(true);
                     SpeedrunRoulette.LOGGER.info("WorldSelectionOverlay: Found defaultEntryHeight field");
                 } catch (NoSuchFieldException e2) {
                     SpeedrunRoulette.LOGGER.error("WorldSelectionOverlay: itemHeight/defaultEntryHeight field NOT found");
                 }
            }

            // 2. Find scrollAmount (might be in superclass or named differently)
            // Try method first
            try {
                getScrollAmountMethod = listClass.getMethod("getScrollAmount");
                SpeedrunRoulette.LOGGER.info("WorldSelectionOverlay: Found getScrollAmount method");
            } catch (NoSuchMethodException e) {
                // Try finding "scrollAmount" field in hierarchy
                Class<?> currentClass = listClass;
                while (currentClass != null && currentClass != Object.class) {
                    try {
                        Field f = currentClass.getDeclaredField("scrollAmount");
                        f.setAccessible(true);
                        scrollAmountField = f;
                        SpeedrunRoulette.LOGGER.info("WorldSelectionOverlay: Found scrollAmount field in " + currentClass.getName());
                        break; 
                    } catch (NoSuchFieldException ex) {
                        currentClass = currentClass.getSuperclass();
                    }
                }
                if (scrollAmountField == null) {
                     SpeedrunRoulette.LOGGER.error("WorldSelectionOverlay: getScrollAmount method/field NOT found");
                }
            }

        } catch (Exception e) {
            SpeedrunRoulette.LOGGER.error("WorldSelectionOverlay reflection setup failed", e);
        }
    }

    private static class RunStatus {
        String status; // SUCCESS, FAILURE, or NONE
        String objective;
        String time;
        
        RunStatus(String s, String o, String t) {
            this.status = s;
            this.objective = o;
            this.time = t;
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof SelectWorldScreen) {
            STATUS_CACHE.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof SelectWorldScreen screen) {
            WorldSelectionList list = null;
            for (net.minecraft.client.gui.components.events.GuiEventListener child : screen.children()) {
                if (child instanceof WorldSelectionList wsl) {
                    list = wsl;
                    break;
                }
            }
            
            if (list != null && summaryField != null) {
                // SpeedrunRoulette.LOGGER.info("WorldSelectionOverlay: Rendering overlay...");
                renderOverlay(event.getGuiGraphics(), list, event.getMouseX(), event.getMouseY());
            } else {
                SpeedrunRoulette.LOGGER.info("WorldSelectionOverlay: list=" + list + " summaryField=" + summaryField);
            }
        }
    }

    private static void renderOverlay(GuiGraphics g, WorldSelectionList list, int mouseX, int mouseY) {
        try {
            int itemHeight = 36;
            if (itemHeightField != null) {
                try {
                    itemHeight = itemHeightField.getInt(list);
                } catch (Exception e) {}
            }
            
            // List geometry
            int listTop = list.getY(); 
            int listBottom = list.getBottom(); 
            double scroll = 0;
            if (getScrollAmountMethod != null) {
                try {
                    scroll = (double) getScrollAmountMethod.invoke(list);
                } catch (Exception e) {}
            } else if (scrollAmountField != null) {
                try {
                    scroll = scrollAmountField.getDouble(list);
                } catch (Exception e) {}
            }
            
            List<WorldSelectionList.Entry> children = list.children();
            for (int i = 0; i < children.size(); i++) {
                WorldSelectionList.Entry entry = children.get(i);
                if (entry instanceof WorldSelectionList.WorldListEntry worldEntry) {
                    
                    int rowTop = (int)(list.getY() + 4 - scroll + i * itemHeight);
                    
                    // Icon position (relative to row)
                    int iconY = rowTop + 10;
                    int iconHeight = 9; // Standard font height
                    
                    // Strict visibility check: Only draw if the icon is fully within the list bounds
                    // Adding a small padding (e.g. 2 pixels) to make it disappear "earlier" as requested
                    if (iconY >= listTop + 2 && iconY + iconHeight <= listBottom - 2) {
                        renderEntryStatus(g, list, worldEntry, rowTop, mouseX, mouseY);
                    }
                }
            }
        } catch (Exception e) {
            // prevent spam
        }
    }

    private static void renderEntryStatus(GuiGraphics g, WorldSelectionList list, WorldSelectionList.WorldListEntry entry, int y, int mouseX, int mouseY) {
        try {
            LevelSummary summary = (LevelSummary) summaryField.get(entry);
            String id = summary.getLevelId();
            
            RunStatus status = STATUS_CACHE.computeIfAbsent(id, WorldSelectionOverlay::loadStatus);
            
            if (status != null && !"NONE".equals(status.status)) {
                // Heuristic for X: Right side of the row
                int rowWidth = list.getRowWidth();
                int listCenter = g.guiWidth() / 2;
                int x = listCenter + rowWidth / 2 - 20; 
                int iconY = y + 10;
                
                String icon = "NONE";
                int color = 0xFFFFFFFF;
                
                if ("SUCCESS".equals(status.status)) {
                    icon = "✔";
                    color = 0xFF00FF00;
                } else if ("FAILURE".equals(status.status)) {
                    icon = "✘";
                    color = 0xFFFF0000;
                }

                if (!"NONE".equals(icon)) {
                    // Render icon
                    g.drawString(Minecraft.getInstance().font, icon, x, iconY, color, false);

                    // Tooltip
                    if (mouseX >= x && mouseX <= x + 20 && mouseY >= iconY && mouseY <= iconY + 10) {
                        List<Component> tooltip = new java.util.ArrayList<>();
                        if ("SUCCESS".equals(status.status)) {
                            tooltip.add(Component.literal("Objective Completed!").withStyle(ChatFormatting.GREEN));
                            if (status.objective != null && !status.objective.isEmpty()) {
                                tooltip.add(Component.literal("Objective: " + status.objective).withStyle(ChatFormatting.GRAY));
                            }
                            if (status.time != null && !status.time.isEmpty()) {
                                tooltip.add(Component.literal("Time: " + status.time).withStyle(ChatFormatting.YELLOW));
                            }
                        } else {
                            tooltip.add(Component.literal("Speedrun Failed").withStyle(ChatFormatting.RED));
                            tooltip.add(Component.literal("Player quit before completion").withStyle(ChatFormatting.GRAY));
                        }
                        renderTooltip(g, tooltip, mouseX, mouseY);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static void renderTooltip(GuiGraphics guiGraphics, List<Component> text, int x, int y) {
        if (text.isEmpty()) return;
        
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        
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
        
        // Adjust if off screen
        if (bx + totalWidth > width) bx -= (totalWidth + 20);
        if (by + totalHeight > height) by -= totalHeight;
        
        // Background
        guiGraphics.fill(bx, by, bx + totalWidth, by + totalHeight, 0xF0100010);
        guiGraphics.renderOutline(bx, by, totalWidth, totalHeight, 0x505000FF); 
        
        for (int i = 0; i < text.size(); i++) {
            guiGraphics.drawString(font, text.get(i), bx + 4, by + 4 + (i * lineHeight), 0xFFFFFFFF, false);
        }
    }

    private static RunStatus loadStatus(String levelId) {
        try {
            Path saves = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(levelId);
            Path file = saves.resolve("speedrun_status.txt");
            if (Files.exists(file)) {
                List<String> lines = Files.readAllLines(file);
                if (!lines.isEmpty()) {
                    String s = lines.get(0).trim();
                    String o = lines.size() > 1 ? lines.get(1).trim() : "";
                    String t = lines.size() > 2 ? lines.get(2).trim() : "";
                    return new RunStatus(s, o, t);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return new RunStatus("NONE", "", "");
    }
}
