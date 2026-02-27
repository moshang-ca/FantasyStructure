package org.moshang.fantasystructure.menu;

import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.slot.ExtendedFluidTank;
import org.moshang.fantasystructure.blockentity.container.BEFluidBus;
import org.moshang.fantasystructure.registry.FSMenuType;

public class FluidBusMenu extends BaseMenu {
    @Getter
    private final IFluidTransfer fluidHandler;
    @Getter
    private final IFluidTransfer filterHandler;

    public FluidBusMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory playerInv, BlockPos pos) {
        super(pMenuType, pContainerId, pos);
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if(be instanceof BEFluidBus bus) {
            this.fluidHandler = bus.getFluidTank();
            this.filterHandler = bus.getFilterHandler();
        } else {
            this.fluidHandler = ExtendedFluidTank.create(1, 0);
            this.filterHandler = ExtendedFluidTank.create(1, 0);
            throw new IllegalStateException("Wrong BlockEntity");
        }

        addPlayerInventory(playerInv, 8, 120);
    }

    public FluidBusMenu(int pContainerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(FSMenuType.FLUID_BUS_MENU_TYPE.get(), pContainerId, playerInv, buf.readBlockPos());
    }

    public static FluidBusMenu createForServer(int pContainerId, Inventory playerInv, BlockEntity blockEntity) {
        if(blockEntity instanceof BEFluidBus) {
            return new FluidBusMenu(
                    FSMenuType.FLUID_BUS_MENU_TYPE.get(),
                    pContainerId, playerInv, blockEntity.getBlockPos());
        }
        throw new IllegalStateException("Wrong BlockEntity");
    }

    public int getTanks() {
        return this.fluidHandler.getTanks();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
