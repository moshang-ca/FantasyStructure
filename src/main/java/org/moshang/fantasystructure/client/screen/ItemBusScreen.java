package org.moshang.fantasystructure.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.menu.ItemBusMenu;

@SuppressWarnings("removal")
public class ItemBusScreen extends AbstractContainerScreen<ItemBusMenu> {
    private final ResourceLocation guiTexture;

    public ItemBusScreen(ItemBusMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.guiTexture = new ResourceLocation(FantasyStructure.MODID, pMenu.getComponentCaps().getGuiTexture());

        this.imageWidth = 176;
        this.imageHeight = 221;
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
        pGuiGraphics.blit(guiTexture, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);
    }
}
