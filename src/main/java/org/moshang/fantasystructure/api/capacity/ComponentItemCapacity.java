package org.moshang.fantasystructure.api.capacity;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ComponentItemCapacity implements StringRepresentable {
    TINY(4),
    SMALL(8),
    MEDIUM(32),
    LARGE(56),
    GREAT(112),
    ENDLESS(560);


    private final int slots;

    ComponentItemCapacity(int size) {
        this.slots = size;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase();
    }
}
