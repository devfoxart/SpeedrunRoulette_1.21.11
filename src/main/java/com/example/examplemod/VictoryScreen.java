package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;

public class VictoryScreen extends Screen {
    public VictoryScreen() {
        super(Component.literal("Victory"));
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;
        int startY = this.height - 110; // Moved up to fit 4th button

        // Rejouer (Même Objectif)
        this.addRenderableWidget(Button.builder(Component.literal("Rejouer (Même Objectif)"), (btn) -> {
            SpeedrunState.prepareForRetry();
            
            // Standard "Save and Quit" logic
            boolean isSingleplayer = this.minecraft.isLocalServer();
            // In 1.21, we don't call level.disconnect() directly usually?
            // PauseScreen uses:
            // boolean flag = this.minecraft.isLocalServer();
            // this.minecraft.level.disconnect();
            // if (flag) { this.minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel"))); }
            // else { this.minecraft.disconnect(); }
            
            // Wait, compiler says disconnect(Screen) expects 2 args: Screen, boolean.
            // So:
            
            if (this.minecraft.level != null) {
                // disconnect() in ClientLevel needs a Component for reason?
                // Or maybe we don't need to call level.disconnect() manually if we call minecraft.disconnect()?
                // PauseScreen usually calls level.disconnect() first.
                // Let's try passing a component.
                this.minecraft.level.disconnect(Component.translatable("menu.disconnect"));
            }
            
            if (isSingleplayer) {
                // For singleplayer, passing a screen to disconnect() shows it during saving.
                // However, if the game doesn't automatically switch to TitleScreen afterwards, we might be stuck.
                // To be safe, let's use the TitleScreen directly. 
                // The saving will still happen in background, and the user will see the Title Screen immediately.
                // This prevents being stuck on "Saving Level" indefinitely if the callback fails.
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            } else {
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            }
        }).bounds(this.width / 2 - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        // Nouvelle Run (Nouveaux Objectifs)
        this.addRenderableWidget(Button.builder(Component.literal("Nouvelle Run"), (btn) -> {
            SpeedrunState.prepareForNewGame();
            
            boolean isSingleplayer = this.minecraft.isLocalServer();
             if (this.minecraft.level != null) {
                this.minecraft.level.disconnect(Component.translatable("menu.disconnect"));
            }
            
            if (isSingleplayer) {
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            } else {
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            }
        }).bounds(this.width / 2 - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build());

        // Menu Principal
        this.addRenderableWidget(Button.builder(Component.literal("Menu Principal"), (btn) -> {
            boolean isSingleplayer = this.minecraft.isLocalServer();
             if (this.minecraft.level != null) {
                this.minecraft.level.disconnect(Component.translatable("menu.disconnect"));
            }
            
            if (isSingleplayer) {
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            } else {
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            }
        }).bounds(this.width / 2 - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight).build());

        // Rester en Jeu (Fermer Menu)
        this.addRenderableWidget(Button.builder(Component.literal("Rester en Jeu"), (btn) -> {
            this.onClose();
        }).bounds(this.width / 2 - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight).build());
    }


    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESCAPE
            this.onClose();
            return true;
        }
        return false;
    }
    
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. Background
        this.renderTransparentBackground(g);
        g.fill(0, 0, this.width, this.height, 0xAA000000); // Darken background
        
        // 2. Render Buttons (via super)
        super.render(g, mouseX, mouseY, partialTick);
        
        // 3. Render Victory Content
        int centerX = this.width / 2;
        int currentY = 20;

        // Title
        g.drawCenteredString(this.font, Component.literal("VICTOIRE !").withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.GOLD), centerX, currentY, 0xFFD700);
        currentY += 25;

        // Retrieve Objective Data
        java.util.List<Objective> objs = SpeedrunState.getObjectives();
        net.minecraft.world.item.ItemStack icon = net.minecraft.world.item.ItemStack.EMPTY;
        Component objNameComp = Component.literal("Objectif Inconnu");

        if (objs != null && !objs.isEmpty()) {
            Objective obj = objs.get(0);
            icon = obj.getIcon();
            if (objs.size() > 1) {
                 objNameComp = Component.literal("Liste de " + objs.size() + " items");
            } else {
                 objNameComp = obj.getDisplayName();
            }
        } else if (SpeedrunRoulette.pendingVictoryObjectiveName != null) {
            objNameComp = Component.literal(SpeedrunRoulette.pendingVictoryObjectiveName);
        }

        // Render BIG Item Icon (Scale 4.0 = 64x64)
        if (!icon.isEmpty()) {
            float scale = 4.0f;
            // Manual Pose Stack (No push/pop available)
            g.pose().translate((float)centerX, (float)(currentY + 32));
            g.pose().scale(scale, scale);
            g.pose().translate(-8.0f, -8.0f);
            
            g.renderItem(icon, 0, 0);
            
            // Reverse
            g.pose().translate(8.0f, 8.0f);
            g.pose().scale(1.0f/scale, 1.0f/scale);
            g.pose().translate(-(float)centerX, -(float)(currentY + 32));
            
            currentY += 70; // 64 + padding
        } else {
            currentY += 10;
        }

        // Render Objective Name (WHITE)
        g.drawCenteredString(this.font, objNameComp, centerX, currentY, 0xFFFFFFFF);
        currentY += 15;

        // Render Time (GREEN)
        String time = SpeedrunRoulette.pendingVictoryTime;
        if (time == null) time = "--:--";
        
        float timeScale = 2.0f;
        g.pose().translate((float)centerX, (float)(currentY + 5));
        g.pose().scale(timeScale, timeScale);
        g.drawCenteredString(this.font, time, 0, 0, 0xFF55FF55);
        g.pose().scale(1.0f/timeScale, 1.0f/timeScale);
        g.pose().translate(-(float)centerX, -(float)(currentY + 5));
        
        currentY += 30;

        // Render Stats (Morts, Distance, Jours)
        g.drawCenteredString(this.font, Component.literal("Statistiques").withStyle(net.minecraft.ChatFormatting.UNDERLINE), centerX, currentY, 0xFFAAAAAA);
        currentY += 15;
        
        String deathStr = "Morts : " + SpeedrunState.getDeathCount();
        String distStr = "Distance : " + (int)SpeedrunState.getTraveledMeters() + "m";
        String daysStr = "Jours : " + SpeedrunState.getDaysPlayed();
        
        g.drawCenteredString(this.font, deathStr, centerX, currentY, 0xFFFFFFFF);
        currentY += 12;
        g.drawCenteredString(this.font, distStr, centerX, currentY, 0xFFFFFFFF);
        currentY += 12;
        g.drawCenteredString(this.font, daysStr, centerX, currentY, 0xFFFFFFFF);
        
        // Render Splits (Optional/Secondary)
        currentY += 20;
        java.util.Map<String, String> splits = SpeedrunState.getSplits();
        if (!splits.isEmpty()) {
             g.drawCenteredString(this.font, Component.literal("Temps Intermédiaires").withStyle(net.minecraft.ChatFormatting.UNDERLINE), centerX, currentY, 0xFFAAAAAA);
             currentY += 15;
             for (java.util.Map.Entry<String, String> entry : splits.entrySet()) {
                 g.drawCenteredString(this.font, entry.getKey() + ": " + entry.getValue(), centerX, currentY, 0xFFDDDDDD);
                 currentY += 12;
             }
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        // Do nothing, we handle background in render()
    }
}
