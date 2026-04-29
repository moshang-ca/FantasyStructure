package org.moshang.fantasystructure.api.capability.recipe;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
public enum IO implements StringRepresentable {
    IN("input"),
    OUT("output");


    private final String name;
    IO(String name) {
        this.name = name;
    }

    public boolean support(IO io) {
        return this == io;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return name;
    }
}
