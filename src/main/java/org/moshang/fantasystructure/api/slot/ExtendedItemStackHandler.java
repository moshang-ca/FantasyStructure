package org.moshang.fantasystructure.api.slot;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class ExtendedItemStackHandler extends ItemStackHandler implements ITagSerializable<CompoundTag>, IContentChangeAware {
    private Runnable onContentsChanged = () -> {};

    public ExtendedItemStackHandler(int size) {
        super(size);
    }

    public ExtendedItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    public void updateStacks(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
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
