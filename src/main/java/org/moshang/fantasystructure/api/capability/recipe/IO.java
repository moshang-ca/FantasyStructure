package org.moshang.fantasystructure.api.capability.recipe;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;

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
    public String getSerializedName() {
        return name;
    }
}
