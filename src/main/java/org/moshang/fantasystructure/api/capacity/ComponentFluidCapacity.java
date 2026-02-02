package org.moshang.fantasystructure.api.capacity;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ComponentFluidCapacity implements StringRepresentable {
    TINY(1, 8000),
    SMALL(1, 12000),
    MEDIUM(3, 32000),
    LARGE(5, 128000),
    GREAT(9, 256000),
    GIANT(9, 512000),
    COLOSSAL(9, 1024000),
    TITANIC(9, 4096000),
    ENDLESS(540, Integer.MAX_VALUE);


    private final int slots;
    private final int maxCapacity;

    ComponentFluidCapacity(int size, int maxCapacity) {
        this.slots = size;
        this.maxCapacity = maxCapacity;
    }

    public int getSlots() {
        return slots;
    }
    public int getMaxCapacity() {
        return maxCapacity;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
