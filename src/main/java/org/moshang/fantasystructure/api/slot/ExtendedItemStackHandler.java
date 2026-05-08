package org.moshang.fantasystructure.api.slot;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Setter;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class ExtendedItemStackHandler extends ItemStackHandler implements ITagSerializable<CompoundTag>, IContentChangeAware {
    private Runnable onContentsChanged = () -> {};
    @Setter
    private Predicate<ItemStack> filter;

    public ExtendedItemStackHandler(int size) {
        super(size);
    }

    public ExtendedItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    public ExtendedItemStackHandler(NonNullList<ItemStack> stacks, Predicate<ItemStack> filter) {
        super(stacks);
        this.filter = filter;
    }

    public IItemTransfer toIItemTransfer() {
        return ItemTransferHelperImpl.toItemTransfer(this);
    }

    public void updateStacks(NonNullList<ItemStack> stacks) {
        for(int i = 0; i < this.stacks.size(); i++) {
            if(i < stacks.size()) {
                this.stacks.set(i, stacks.get(i));
            } else {
                this.stacks.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if(filter != null) {
            return filter.test(stack) && super.isItemValid(slot, stack);
        }
        return super.isItemValid(slot, stack);
    }

    @Override
    public void setOnContentsChanged(Runnable onContentChanged) {
        this.onContentsChanged = onContentChanged;
    }

    @Override
    public Runnable getOnContentsChanged() {
        return onContentsChanged;
    }
}
