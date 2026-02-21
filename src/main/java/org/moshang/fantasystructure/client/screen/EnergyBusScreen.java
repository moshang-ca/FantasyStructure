package org.moshang.fantasystructure.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.menu.EnergyBusMenu;

@SuppressWarnings("removal")
public class EnergyBusScreen extends AbstractContainerScreen<EnergyBusMenu> {
    private static final ResourceLocation GUI = new ResourceLocation(FantasyStructure.MODID, "textures/gui/gui_bar.png");

    private static final int ENERGY_BAR_HEIGHT = 61;
    private static final int ENERGY_BAR_WIDTH = 20;
    private static final int ENERGY_BAR_X_OFFSET_IN_GUI = 78;
    private static final int ENERGY_BAR_Y_OFFSET_IN_GUI = 10;
    private static final int ENERGY_BAR_X_OFFSET_IN_TEX = 196;

    public EnergyBusScreen(EnergyBusMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
        renderEnergyTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        pGuiGraphics.blit(GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        renderBar(pGuiGraphics);
    }

    private void renderBar(GuiGraphics pGuiGraphics) {
        float percentage = menu.getEnergyPercentage();
        int fillHeight = Math.round(ENERGY_BAR_HEIGHT * percentage);

        int barX = this.leftPos + ENERGY_BAR_X_OFFSET_IN_GUI;
        int barY = this.topPos + ENERGY_BAR_Y_OFFSET_IN_GUI + (ENERGY_BAR_HEIGHT - fillHeight);

        pGuiGraphics.blit(GUI, barX, barY,
                ENERGY_BAR_X_OFFSET_IN_TEX, ENERGY_BAR_HEIGHT - fillHeight,
                ENERGY_BAR_WIDTH, fillHeight);
    }

    private void renderEnergyTooltip(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        if(isMouseOverBar(pMouseX, pMouseY)) {
            int stored = menu.getEnergyStored();
            int max = menu.getMaxEnergyStored();
            float percentage = menu.getEnergyPercentage();

            String text = String.format("%,d / %,d FE (%.1f%%)", stored, max, percentage * 100);
            pGuiGraphics.renderTooltip(this.font, Component.literal(text), pMouseX, pMouseY);
        }
    }

    private boolean isMouseOverBar(int pMouseX, int pMouseY) {
        int barX = this.leftPos + ENERGY_BAR_X_OFFSET_IN_GUI;
        int barY = this.topPos + ENERGY_BAR_Y_OFFSET_IN_GUI;

        return pMouseX >= barX && pMouseX < barX + ENERGY_BAR_WIDTH &&
                pMouseY >= barY && pMouseY < barY + ENERGY_BAR_HEIGHT;
    }
}
