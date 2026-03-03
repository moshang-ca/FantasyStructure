package org.moshang.fantasystructure.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.client.screen.widget.FilterFluidTankWidget;
import org.moshang.fantasystructure.client.screen.widget.FluidTankWidget;
import org.moshang.fantasystructure.menu.FluidBusMenu;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class FluidBusScreen extends AbstractContainerScreen<FluidBusMenu> {
    private static final ResourceLocation GUI = new ResourceLocation(FantasyStructure.MODID, "textures/gui/gui_fluid_bar.png");
    private static final int FIRST_FLUID_BAR_X = 13;
    private static final int FIRST_FLUID_BAR_Y = 43;
    private static final int BAR_X = 177;

    private final List<FluidTankWidget> tankWidgets = new ArrayList<>();
    private final List<FluidTankWidget> filterWidgets = new ArrayList<>();

    public FluidBusScreen(FluidBusMenu container, Inventory inventory, Component component) {
        super(container, inventory, component);

        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();

        this.tankWidgets.clear();
        this.filterWidgets.clear();
        for(int i = 0; i < menu.getTanks(); ++i) {
            var tankWidget = new FluidTankWidget(
                    FIRST_FLUID_BAR_X + 22 * i, FIRST_FLUID_BAR_Y,
                    16, 61, null, menu::getFluidHandler, i, menu.getPos(), this);
            this.tankWidgets.add(tankWidget);
            addRenderableWidget(tankWidget);
        }

        for(int i = 0; i < menu.getFluidHandler().getTanks(); ++i) {
            var filterWidget = new FilterFluidTankWidget(
                    1 + FIRST_FLUID_BAR_X + 22 * i, FIRST_FLUID_BAR_Y - 22,
                    16, 16, null, menu::getFluidHandler, i, menu.getPos(), this);
            this.filterWidgets.add(filterWidget);
            addRenderableWidget(filterWidget);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        // Tank widget background
        for(int i  = 0; i < menu.getTanks(); ++i) {
            guiGraphics.blit(GUI, leftPos + FIRST_FLUID_BAR_X + 22 * i, topPos + FIRST_FLUID_BAR_Y,
                    BAR_X, 0, 18, 62);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // Filter slot background
        for(int i = 0; i < menu.getFluidHandler().getTanks(); ++i) {
            guiGraphics.blit(GUI, leftPos + FIRST_FLUID_BAR_X + 22 * i, topPos + FIRST_FLUID_BAR_Y - 23,
                    BAR_X + 18, 0, 18, 18);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        tankWidgets.stream().filter(widget -> widget.isMouseOver(pX, pY))
                .findFirst()
                .ifPresent(widget -> pGuiGraphics.renderTooltip(font,
                        widget.getFluidDisplayName().copy().append(": " + widget.getAmount() + " / " + widget.getCapacity()),
                        pX, pY));

        filterWidgets.stream().filter(widget -> widget.isMouseOver(pX, pY)).findFirst()
                .ifPresent(widget -> pGuiGraphics.renderTooltip(font, widget.getFluidDisplayName(), pX, pY));
    }
}
