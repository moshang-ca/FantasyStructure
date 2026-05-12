package org.moshang.fantasystructure.client.widget;

import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.PhantomTankWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.utils.Position;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.slot.ExtendedFluidTank;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class FilterableTankWidget extends TankWidget {
    private static final ResourceTexture GUI_OVERLAY = new ResourceTexture(FantasyStructure.id("textures/gui/gui_fluid_bar.png"));

    private final PhantomTankWidget filter;

    public FilterableTankWidget(IFluidTransfer fluidTank, int tank, int x, int y,
                                boolean allowClickContainerFilling, boolean allowClickContainerEmptying,
                                PhantomTankWidget filter) {
        super(fluidTank, tank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
        setShowAmount(false);
        setOverlay(GUI_OVERLAY.getSubTexture(0, 0, .5f, 1));
        this.filter = filter;
        filter.setSelfPosition(x, y - 22);
        if(fluidTank instanceof ExtendedFluidTank exFluidTank) {
            filter.setIFluidStackUpdater(fluidStack -> exFluidTank.setValidatorInTank(tank, fluidStack));
        }
        filter.setShowAmount(false).setDrawHoverTips(false).setOverlay(GUI_OVERLAY.getSubTexture(.5f, 0, .5f, 0.28125));
    }

    @Override
    public void setSelfPosition(Position selfPosition) {
        filter.setSelfPosition(new Position(selfPosition.x, selfPosition.y - 18));
        super.setSelfPosition(selfPosition);
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        filter.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        filter.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }
}
