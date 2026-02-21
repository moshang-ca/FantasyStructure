package org.moshang.fantasystructure.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.BlockEntityEnergyBusBase;
import org.moshang.fantasystructure.registry.FSMenuType;

public class EnergyBusMenu extends BaseMenu {
    private final BlockEntityEnergyBusBase blockEntity;

    private int cachedEnergy = -1;

    public EnergyBusMenu(int pContainerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(
                FSMenuType.ENERGY_BUS_MENU_TYPE.get(),
                pContainerId, playerInv, buf.readBlockPos()
        );
    }

    public EnergyBusMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory playerInv, BlockPos pos) {
        super(pMenuType, pContainerId, pos);
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if(be instanceof BlockEntityEnergyBusBase energyBus) {
            this.blockEntity = energyBus;
        } else {
            this.blockEntity = null;
            throw new IllegalStateException("Wrong BlockEntity");
        }

        addPlayerInventory(playerInv, 7, 83);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getEnergyStored();
            }

            @Override
            public void set(int pValue) {
                cachedEnergy = pValue;
            }
        });
    }

    public static EnergyBusMenu createForServer(int pContainerId, Inventory playerInv, BlockEntity blockEntity) {
        if(blockEntity instanceof BlockEntityEnergyBusBase be) {
            return new EnergyBusMenu(
                    FSMenuType.ENERGY_BUS_MENU_TYPE.get(),
                    pContainerId, playerInv, be.getBlockPos()
            );
        }
        return null;
    }

    public int getEnergyStored() {
        if(this.blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            return this.blockEntity.getEnergyStored();
        } else {
            return cachedEnergy;
        }
    }

    public int getMaxEnergyStored() {
        return blockEntity.getCapacity();
    }

    public float getEnergyPercentage() {
        int max = getMaxEnergyStored();
        if(max == 0) return 0;
        return getEnergyStored() / (float)max;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        int curEnergy = blockEntity.getEnergyStored();
        if(curEnergy != cachedEnergy) {
            cachedEnergy = curEnergy;
        }
        super.broadcastChanges();
    }
}
