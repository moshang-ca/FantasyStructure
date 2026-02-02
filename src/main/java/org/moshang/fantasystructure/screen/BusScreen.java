package org.moshang.fantasystructure.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.menu.BusMenu;

@SuppressWarnings("removal")
public class BusScreen extends AbstractContainerScreen<BusMenu> {
    private final ResourceLocation guiTexture;

    public BusScreen(BusMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.guiTexture = new ResourceLocation(FantasyStructure.MODID, pMenu.getComponentCaps().getGuiTexture());

        this.imageWidth = 176;
        this.imageHeight = 221;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        pGuiGraphics.blit(guiTexture, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
    }
}
