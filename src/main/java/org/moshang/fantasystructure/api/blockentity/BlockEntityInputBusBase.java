package org.moshang.fantasystructure.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.block.BlockInputBusBase;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.slot.ExtendedItemStackHandler;

public abstract class BlockEntityInputBusBase extends BlockEntity {
    private final ExtendedItemStackHandler itemHandler;
    private final LazyOptional<IItemHandler> handler;

    public BlockEntityInputBusBase(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        ComponentItemCapacity type = pBlockState.getValue(BlockInputBusBase.TYPE);
        this.itemHandler = createHandler(type.getSlots(), type.getMaxStackSize());
        this.handler = LazyOptional.of(() -> itemHandler);
    }

    private ExtendedItemStackHandler createHandler(int size, int maxStackSize) {
        return new ExtendedItemStackHandler(size, maxStackSize) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.itemHandler.deserializeNBT(pTag.getCompound("itemHandler"));
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("itemHandler", itemHandler.serializeNBT());
    }

/*    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            if(!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return itemHandler.getStackInSlot(pSlot);
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return itemHandler.extractItem(pSlot, pAmount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        ItemStack stack = getItem(pSlot);
        if(stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setItem(pSlot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        itemHandler.setStackInSlot(pSlot, pStack);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if(level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return pPlayer.distanceToSqr(worldPosition.getX() + .5d,
                worldPosition.getY() + .5d,
                worldPosition.getZ() + .5d) <= 8;
    }

    @Override
    public void clearContent() {
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }*/

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    public ExtendedItemStackHandler getItemHandler() { return itemHandler; }
}
