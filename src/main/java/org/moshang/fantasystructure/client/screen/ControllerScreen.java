package org.moshang.fantasystructure.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.menu.ControllerMenu;

@SuppressWarnings("removal")
public class ControllerScreen extends AbstractContainerScreen<ControllerMenu> {
    private static final ResourceLocation GUI = new ResourceLocation(FantasyStructure.MODID, "textures/gui/controller.png");

    public ControllerScreen(ControllerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 237;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        pGuiGraphics.blit(GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(pGuiGraphics, pMouseX, pMouseY);

        pGuiGraphics.drawString(font, Component.translatable(menu.getStructureID().toLanguageKey()), 21, 20, 0xFFFFFFFF);
        pGuiGraphics.drawString(font, menu.isFormed() ? "Formed" : "Not formed", 21, 32, 0xFFFFFFFF);
    }
}
