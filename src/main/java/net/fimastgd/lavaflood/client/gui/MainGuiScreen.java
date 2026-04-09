package net.fimastgd.lavaflood.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;

import net.fimastgd.lavaflood.world.inventory.MainGuiMenu;
import net.fimastgd.lavaflood.network.MainGuiButtonMessage;
import net.fimastgd.lavaflood.LavafloodMod;

import java.util.HashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

public class MainGuiScreen extends AbstractContainerScreen<MainGuiMenu> {
    private final static HashMap<String, Object> guistate = MainGuiMenu.guistate;
    private final Level world;
    private final int x, y, z;
    private final Player entity;

    public MainGuiScreen(MainGuiMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 263;
        this.imageHeight = 116;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderTooltip(ms, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack ms, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        String title = "Welcome to LavaFlood!";
        int titleWidth = this.font.width(title);
        this.font.draw(poseStack, title, (imageWidth - titleWidth) / 2.0f, 13, -1);

        String subtitle = "To protect your world, click on one of the buttons";
        int subtitleWidth = this.font.width(subtitle);
        this.font.draw(poseStack, subtitle, (imageWidth - subtitleWidth) / 2.0f, 28, -65536);
    }

    @Override
    public void onClose() {
        super.onClose();
        Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void init() {
        super.init();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

        int button1Width = 124;
        int button1X = this.leftPos + (imageWidth - button1Width) / 2;
        this.addRenderableWidget(new Button(button1X, this.topPos + 55, button1Width, 20,
                new TextComponent("Start lava flooding"), e -> {
            if (true) {
                LavafloodMod.PACKET_HANDLER.sendToServer(new MainGuiButtonMessage(0, x, y, z));
                MainGuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
            }
        }));

        int button2Width = 160;
        int button2X = this.leftPos + (imageWidth - button2Width) / 2;
        this.addRenderableWidget(new Button(button2X, this.topPos + 83, button2Width, 20,
                new TextComponent("Play without lava flooding"), e -> {
            if (true) {
                LavafloodMod.PACKET_HANDLER.sendToServer(new MainGuiButtonMessage(1, x, y, z));
                MainGuiButtonMessage.handleButtonAction(entity, 1, x, y, z);
            }
        }));
    }
}