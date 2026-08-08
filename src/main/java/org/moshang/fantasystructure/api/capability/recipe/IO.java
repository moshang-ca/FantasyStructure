/*
 * Copyright (C) 2026 moshang
 *
 * This file is part of FantasyStructure.
 * Contains code adapted from Multiblocked2 (LGPL-3.0).
 *
 * FantasyStructure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */

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
