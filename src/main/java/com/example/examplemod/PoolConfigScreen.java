package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PoolConfigScreen extends Screen {
    private final Screen parent;

    public PoolConfigScreen(Screen parent) {
        super(Component.translatable("gui.examplemod.pool_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6;
        int x = this.width / 2 - 100;
        int w = 200;
        int h = 20;
        int gap = 24;

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.examplemod.pool_config.items", (Config.ENABLE_ITEMS.get() ? Component.translatable("gui.examplemod.yes") : Component.translatable("gui.examplemod.no"))),
            (btn) -> {
                boolean newState = !Config.ENABLE_ITEMS.get();
                if (!newState && !Config.ENABLE_BLOCKS.get() && !Config.ENABLE_ADVANCEMENTS.get()) {
                     // Prevent disabling all
                     return;
                }
                Config.ENABLE_ITEMS.set(newState);
                btn.setMessage(Component.translatable("gui.examplemod.pool_config.items", (Config.ENABLE_ITEMS.get() ? Component.translatable("gui.examplemod.yes") : Component.translatable("gui.examplemod.no"))));
            }
        ).bounds(x, y, w, h).build());

        y += gap;
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.examplemod.pool_config.blocks", (Config.ENABLE_BLOCKS.get() ? Component.translatable("gui.examplemod.yes") : Component.translatable("gui.examplemod.no"))),
            (btn) -> {
                boolean newState = !Config.ENABLE_BLOCKS.get();
                if (!newState && !Config.ENABLE_ITEMS.get() && !Config.ENABLE_ADVANCEMENTS.get()) {
                     // Prevent disabling all
                     return;
                }
                Config.ENABLE_BLOCKS.set(newState);
                btn.setMessage(Component.translatable("gui.examplemod.pool_config.blocks", (Config.ENABLE_BLOCKS.get() ? Component.translatable("gui.examplemod.yes") : Component.translatable("gui.examplemod.no"))));
            }
        ).bounds(x, y, w, h).build());

        y += gap;
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.examplemod.pool_config.advancements", (Config.ENABLE_ADVANCEMENTS.get() ? Component.translatable("gui.examplemod.yes") : Component.translatable("gui.examplemod.no"))),
            (btn) -> {
                boolean newState = !Config.ENABLE_ADVANCEMENTS.get();
                if (!newState && !Config.ENABLE_ITEMS.get() && !Config.ENABLE_BLOCKS.get()) {
                     // Prevent disabling all
                     return;
                }
                Config.ENABLE_ADVANCEMENTS.set(newState);
                btn.setMessage(Component.translatable("gui.examplemod.pool_config.advancements", (Config.ENABLE_ADVANCEMENTS.get() ? Component.translatable("gui.examplemod.yes") : Component.translatable("gui.examplemod.no"))));
            }
        ).bounds(x, y, w, h).build());

        y += gap;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.pool_config.customize"), (btn) -> {
            Config.SPEC.save(); // Save config before customizing
            net.minecraft.client.Minecraft.getInstance().setScreen(new PoolCustomizationScreen(this, Config.ENABLE_ITEMS.get(), Config.ENABLE_BLOCKS.get(), Config.ENABLE_ADVANCEMENTS.get()));
        }).bounds(x, y, w, h).build());

        y += gap * 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.examplemod.done"), (btn) -> {
            Config.SPEC.save();
            this.onClose();
        }).bounds(x, y, w, h).build());
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            net.minecraft.client.Minecraft.getInstance().setScreen(this.parent);
        } else {
            super.onClose();
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}
