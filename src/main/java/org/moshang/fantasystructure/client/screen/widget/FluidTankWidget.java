package org.moshang.fantasystructure.client.screen.widget;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.moshang.fantasystructure.network.FSMessages;
import org.moshang.fantasystructure.network.data.TankWidgetClickPacket;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class FluidTankWidget extends AbstractWidget {
    protected final Supplier<IFluidTransfer> handler;
    protected final AbstractContainerScreen<?> screen;
    @Getter
    protected final int tank;
    protected final BlockPos pos;
    private final int offsetX;
    private final int offsetY;

    @SuppressWarnings("DataFlowIssue")
    public FluidTankWidget(int x, int y, int width, int height, @Nullable Component component,
                           Supplier<IFluidTransfer> handler, int tank, BlockPos pos, AbstractContainerScreen<?> screen) {
        super(0, 0, width, height, component);
        this.offsetX = x;
        this.offsetY = y;
        this.handler = handler;
        this.tank = tank;
        this.pos = pos;
        this.screen = screen;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.setX(offsetX + screen.getGuiLeft());
        this.setY(offsetY + screen.getGuiTop());
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        FluidStack fluidStack = this.handler.get().getFluidInTank(tank);

        if(!fluidStack.isEmpty()) {
            TextureAtlasSprite sprite = getFluidSprite(fluidStack);
            int fluidColor = getFluidColor(fluidStack);

            long tankCapacity = getCapacity();
            int filledHeight = (int) ((float) fluidStack.getAmount() / tankCapacity * height);

            renderFluid(guiGraphics, sprite, fluidColor, getX(), getY() + (height - filledHeight), width, filledHeight);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput elementOutput) {}

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if(!isMouseOver(pMouseX, pMouseY)) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return true;

        ItemStack heldItem = mc.player.containerMenu.getCarried();
        if(heldItem.isEmpty()) return true;

        sendClickPacket(pButton == 0);

        return true;
    }

    protected void sendClickPacket(boolean isFill) {
        FSMessages.sendToServer(new TankWidgetClickPacket(pos, tank, isFill));
    }

    private void renderFluid(GuiGraphics guiGraphics, TextureAtlasSprite sprite,
                             int color, int x, int y, int width, int height) {
        float r = ((color) >> 16 & 0xFF) / 255.0F;
        float g = ((color) >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color) >> 24 & 0xFF) / 255.0F;
        if(a == 0) a = 1.f;

        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        // Tile rendering
        for(int tileY = 0; tileY < height; tileY += 16) {
            for(int tileX = 0; tileX < width; tileX += 16) {
                int drawWidth = Math.min(width, width - tileX);
                int drawHeight = Math.min(height, height - tileY);
                guiGraphics.blit(x + tileX, y + tileY, 100, drawWidth, drawHeight, sprite);
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private TextureAtlasSprite getFluidSprite(FluidStack fluidStack) {
        FluidType fluid = fluidStack.getFluid().getFluidType();
        ResourceLocation texture = IClientFluidTypeExtensions.of(fluid).getStillTexture();
        return Minecraft.getInstance().getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(texture);
    }

    private int getFluidColor(FluidStack fluidStack) {
        FluidType fluid = fluidStack.getFluid().getFluidType();
        return IClientFluidTypeExtensions.of(fluid).getTintColor();
    }

    public long getCapacity() {
        return handler.get().getTankCapacity(tank);
    }

    public long getAmount() {
        return handler.get().getFluidInTank(tank).getAmount();
    }

    public Component getFluidDisplayName() {
        return handler.get().getFluidInTank(tank).getDisplayName();
    }
}
