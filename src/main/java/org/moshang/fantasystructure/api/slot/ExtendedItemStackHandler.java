package org.moshang.fantasystructure.api.slot;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class ExtendedItemStackHandler extends ItemStackHandler {
    private final int maxStackSize;

    public ExtendedItemStackHandler(int size, int maxStackSize) {
        super(size);
        this.maxStackSize = maxStackSize;
    }

    @Override
    public int getSlotLimit(int slot) {
        return maxStackSize;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        super.getStackLimit(slot, stack);
        return getSlotLimit(slot);
    }
}
