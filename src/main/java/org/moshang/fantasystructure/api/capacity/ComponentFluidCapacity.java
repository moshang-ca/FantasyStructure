package org.moshang.fantasystructure.api.capacity;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ComponentFluidCapacity implements StringRepresentable {
    TINY(1, 8000),
    SMALL(1, 12000),
    MEDIUM(3, 32000),
    LARGE(5, 128000),
    GREAT(7, 256000),
    GIANT(7, 512000),
    COLOSSAL(7, 1024000),
    TITANIC(7, 4096000),
    ENDLESS(540, Integer.MAX_VALUE); // The Endless will be designed in a scrollable widget.


    private final int tanks;
    private final int maxCapacity;

    ComponentFluidCapacity(int size, int maxCapacity) {
        this.tanks = size;
        this.maxCapacity = maxCapacity;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
