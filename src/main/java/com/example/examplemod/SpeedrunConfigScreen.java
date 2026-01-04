package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class SpeedrunConfigScreen extends Screen {
    private final Screen parent;

    public SpeedrunConfigScreen(Screen parent) {
        super(Component.literal("Speedrun Config"));
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
            Component.literal("Auto Open Wheel: " + (Config.AUTO_OPEN_WHEEL.get() ? "ON" : "OFF")),
            (btn) -> {
                Config.AUTO_OPEN_WHEEL.set(!Config.AUTO_OPEN_WHEEL.get());
                btn.setMessage(Component.literal("Auto Open Wheel: " + (Config.AUTO_OPEN_WHEEL.get() ? "ON" : "OFF")));
            }
        ).bounds(x, y, w, h).build());

        y += gap;
        this.addRenderableWidget(Button.builder(
            Component.literal("Nombre d'objectifs: " + Config.OBJECTIVE_COUNT.get()),
            (btn) -> {
                int current = Config.OBJECTIVE_COUNT.get();
                int next = current + 1;
                if (next > 10) next = 1;
                Config.OBJECTIVE_COUNT.set(next);
                btn.setMessage(Component.literal("Nombre d'objectifs: " + next));
            }
        ).bounds(x, y, w, h).build());

        y += gap;
        this.addRenderableWidget(Button.builder(
            Component.literal("Configurer le Pool d'objectifs..."),
            (btn) -> {
                this.minecraft.setScreen(new PoolConfigScreen(this));
            }
        ).bounds(x, y, w, h).build());

        y += gap;
        this.addRenderableWidget(Button.builder(
            Component.literal("Configurer le HUD..."),
            (btn) -> {
                this.minecraft.setScreen(new HudConfigScreen(this));
            }
        ).bounds(x, y, w, h).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (btn) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}
