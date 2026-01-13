package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
// import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ReminderScreen extends Screen {

    public ReminderScreen() {
        super(Component.translatable("gui.examplemod.objectives_reminder"));
    }

    @Override
    protected void init() {
        // Request advancement updates
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            for (Objective obj : SpeedrunState.getObjectives()) {
                if (obj.getType() == Objective.Type.ADVANCEMENT) {
                    // Use helper to request update for the specific advancement (finds root automatically)
                    AdvancementProgressHelper.requestUpdate(obj.getId());
                    // Don't break, request updates for all relevant tabs to ensure cache is populated
                }
            }
        }

        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 10;
        
        // Button "Fermer" (Close)
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.close"), (button) -> {
            this.onClose();
        }).bounds(this.width / 2 - buttonWidth / 2, this.height - 40, buttonWidth, buttonHeight).build());
        
        // Button "Abandonner (Nouvel Objectif)"
        // Uses same logic as VictoryScreen "Nouvelle Run"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.give_up"), (button) -> {
            SpeedrunState.prepareForNewGame();
            
            // Rename world with "Echec" before leaving
            // SpeedrunRoulette.pendingLevelNewName logic is handled by SpeedrunServerEvents.onClientPlayerLoggedOut
            // But we need to set the variables first.
            
            // Set variables for failure renaming
            // We can reuse the VictoryScreen logic but set isVictory = false.
            // Actually SpeedrunState has no direct "setFailure" method, but we can set a flag or just let it happen.
            // When we disconnect, if objectives are not complete, it's a failure (or just exit).
            // We want to force the rename.
            
            // Let's set the pending name here manually to be sure
            if (SpeedrunState.getObjectives() != null && !SpeedrunState.getObjectives().isEmpty()) {
                String objPrefix;
                if (SpeedrunState.getObjectives().size() > 1) {
                    objPrefix = Component.translatable("gui.examplemod.list_prefix", SpeedrunState.getObjectives().size()).getString();
                } else {
                    objPrefix = SpeedrunState.getObjectives().get(0).getDisplayName().getString();
                }
                
                // Clean prefix
                objPrefix = objPrefix.replaceAll("[\\\\/:*?\"<>|]", "_");
                
                String suffix = Component.translatable("gui.examplemod.failed_suffix").getString(); // No time
                String newName = objPrefix + suffix;
                newName = newName.replaceAll("[\\\\/:*?\"<>|]", "_");
                
                SpeedrunRoulette.pendingLevelNewName = newName;
            }

            boolean isSingleplayer = this.minecraft.isLocalServer();
             if (this.minecraft.level != null) {
                this.minecraft.level.disconnect(Component.translatable("menu.disconnect"));
            }
            
            if (isSingleplayer) {
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            } else {
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
            }
        }).bounds(this.width / 2 - buttonWidth / 2, this.height - 40 - buttonHeight - spacing, buttonWidth, buttonHeight).build());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESCAPE
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.CLOSED_SCREEN, null));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw full screen dark background for immersion (translucent)
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000);
        
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        
        List<Objective> objectives = SpeedrunState.getObjectives();
        if (objectives.isEmpty()) return;
        
        int centerY = this.height / 2;
        int itemSize = 64; // Large icon size
        int spacing = 32;
        int count = objectives.size();
        
        // Layout logic (similar to WheelScreen result)
        // <= 5 items: 1 row
        // > 5 items: 2 rows (split 5 and count-5)
        
        int row1Count = Math.min(count, 5);
        int row2Count = count > 5 ? count - 5 : 0;
        
        int row1Y = (row2Count > 0) ? centerY - 80 : centerY;
        int row2Y = centerY + 80;
        
        if (objectives.size() == 1) {
            // Detailed view for single objective
            renderDetailedObjective(guiGraphics, objectives.get(0), centerY);
        } else {
            renderRow(guiGraphics, objectives, 0, row1Count, row1Y, itemSize, spacing);
            
            if (row2Count > 0) {
                renderRow(guiGraphics, objectives, 5, row2Count, row2Y, itemSize, spacing);
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderDetailedObjective(GuiGraphics guiGraphics, Objective obj, int centerY) {
        int itemSize = 64;
        int x = this.width / 2;
        int y = 60; // Fixed top position to leave room for criteria
        
        // Draw Icon
        guiGraphics.pose().translate((float)x, (float)y);
        guiGraphics.pose().scale(3.0f, 3.0f); 
        guiGraphics.renderItem(obj.getIcon(), -8, -8);
        guiGraphics.pose().scale(1/3.0f, 1/3.0f);
        guiGraphics.pose().translate(-(float)x, -(float)y);
        
        // Draw Name
        Component name = obj.getDisplayName();
        int nameY = y + 35;
        guiGraphics.drawCenteredString(this.font, name, x, nameY, 0xFFFFFFFF);
        
        // Draw Criteria if available
        if (obj.getType() == Objective.Type.ADVANCEMENT) {
            List<AdvancementProgressHelper.CriteriaInfo> criteria = AdvancementProgressHelper.getCriteria(obj.getId());
            if (!criteria.isEmpty()) {
                int criteriaY = nameY + 25;
                
                // Scale down text to 0.75
                float scale = 0.75f;
                // Original line height was ~12-14, scaled down is ~9-10. Let's use 10 for spacing.
                int lineHeight = 10; 
                
                // Increase bottom margin to avoid button overlap (buttons take ~90px at bottom)
                // height - criteriaY - 100 (margin for buttons)
                int maxPerColumn = (this.height - criteriaY - 100) / lineHeight;
                if (maxPerColumn < 1) maxPerColumn = 1;
                
                int columns = (int)Math.ceil((double)criteria.size() / maxPerColumn);
                // Calculate optimal column width based on content
                int maxTextWidth = 100;
                for (AdvancementProgressHelper.CriteriaInfo info : criteria) {
                    int w = this.font.width("[x] " + info.displayName().getString());
                    if (w > maxTextWidth) maxTextWidth = w;
                }
                // Scale the column width
                int columnWidth = (int)(maxTextWidth * scale) + 15; // Padding
                
                int totalWidth = columns * columnWidth;
                int startX = (this.width - totalWidth) / 2;
                
                // Ensure we don't start off-screen
                if (startX < 10) startX = 10;
                
                for (int i = 0; i < criteria.size(); i++) {
                    AdvancementProgressHelper.CriteriaInfo info = criteria.get(i);
                    int col = i / maxPerColumn;
                    int row = i % maxPerColumn;
                    
                    int drawX = startX + col * columnWidth;
                    int drawY = criteriaY + row * lineHeight;
                    
                    boolean completed = info.completed();
                    int color = completed ? 0xFF55FF55 : 0xFFAAAAAA; // Green or Gray
                    String prefix = completed ? "[x] " : "[ ] ";
                    
                    guiGraphics.pose().translate((float)drawX, (float)drawY);
                    guiGraphics.pose().scale(scale, scale);
                    guiGraphics.drawString(this.font, prefix + info.displayName().getString(), 0, 0, color, false);
                    guiGraphics.pose().scale(1/scale, 1/scale);
                    guiGraphics.pose().translate(-(float)drawX, -(float)drawY);
                }
            }
        }
    }

    private void renderRow(GuiGraphics guiGraphics, List<Objective> objectives, int startIndex, int count, int y, int itemSize, int spacing) {
        int totalRowWidth = count * itemSize + (count - 1) * spacing;
        int startX = (this.width - totalRowWidth) / 2;
        
        for (int i = 0; i < count; i++) {
            Objective obj = objectives.get(startIndex + i);
            int x = startX + i * (itemSize + spacing);
            
            // Draw Icon Scaled
            float cx = x + itemSize/2.0f;
            float cy = y;
            
            guiGraphics.pose().translate(cx, cy);
            guiGraphics.pose().scale(3.0f, 3.0f); 
            guiGraphics.renderItem(obj.getIcon(), -8, -8);
            guiGraphics.pose().scale(1/3.0f, 1/3.0f);
            guiGraphics.pose().translate(-cx, -cy);
            
            // Draw Name
            Component name = obj.getDisplayName();
            int nameY = y + 35;
            int nameWidth = this.font.width(name);
            guiGraphics.drawString(this.font, name, x + itemSize/2 - nameWidth/2, nameY, 0xFFFFFFFF, true);
        }
    }
}
