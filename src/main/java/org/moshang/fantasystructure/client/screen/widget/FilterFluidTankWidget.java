package org.moshang.fantasystructure.client.screen.widget;

import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.moshang.fantasystructure.api.slot.ExtendedFluidTank;
import org.moshang.fantasystructure.network.FSMessages;
import org.moshang.fantasystructure.network.data.FilterTankWidgetClickPacket;

import java.util.function.Supplier;

public class FilterFluidTankWidget extends FluidTankWidget {
    public FilterFluidTankWidget(int x, int y, int width, int height, Component component,
                                 Supplier<IFluidTransfer> handler, int tank, BlockPos pos, AbstractContainerScreen<?> screen) {
        super(x, y, width, height, component, handler, tank, pos, screen);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if(!isMouseOver(pMouseX, pMouseY)) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return true;

        ItemStack heldItem = mc.player.containerMenu.getCarried();
        if(heldItem.isEmpty()) {
            sendClickPacket(Fluids.EMPTY);
            return true;
        }

        heldItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .ifPresent(handler -> sendClickPacket(handler.getFluidInTank(0).getFluid()));

        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if(this.handler.get() instanceof ExtendedFluidTank fluidTank) {
            Fluid fluid = fluidTank.getFilter(tank);
            if(fluid != Fluids.EMPTY) {
                TextureAtlasSprite sprite = getFluidSprite(fluid);
                int fluidColor = getFluidColor(fluid);

                renderFluid(guiGraphics, sprite, fluidColor, getX(), getY(), width, height);
            }
        }
    }

    @Override
    public Component getFluidDisplayName() {
        if(handler.get() instanceof ExtendedFluidTank fluidTank) {
            return fluidTank.getFilter(tank).getFluidType().getDescription();
        }
        return Fluids.EMPTY.getFluidType().getDescription();
    }

    protected void sendClickPacket(Fluid fluid) {
        FSMessages.sendToServer(new FilterTankWidgetClickPacket(this.pos, this.tank, fluid));
    }
}
