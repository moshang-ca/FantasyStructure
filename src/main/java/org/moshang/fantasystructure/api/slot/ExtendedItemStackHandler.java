package org.moshang.fantasystructure.api.slot;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;

public class ExtendedItemStackHandler extends ItemStackHandler implements ITagSerializable<CompoundTag>, IContentChangeAware {
    private Runnable onContentsChanged = () -> {};

    public ExtendedItemStackHandler(int size) {
        super(size);
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
