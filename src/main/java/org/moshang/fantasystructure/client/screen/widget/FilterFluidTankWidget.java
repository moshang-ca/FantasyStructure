package org.moshang.fantasystructure.client.screen.widget;

import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
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
            sendClickPacket(null);
            return true;
        }

        heldItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .ifPresent(handler -> sendClickPacket(handler.getFluidInTank(0).getFluid()));

        return true;
    }

    protected void sendClickPacket(Fluid fluid) {
        FSMessages.sendToServer(new FilterTankWidgetClickPacket(this.pos, this.tank, fluid));
    }
}
