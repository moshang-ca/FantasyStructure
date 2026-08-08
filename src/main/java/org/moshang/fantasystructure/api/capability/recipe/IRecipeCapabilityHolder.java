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

import com.google.common.collect.Table;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IRecipeCapabilityHolder {
    @NotNull Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> getRecipeCapabilitiesProxy();

    default boolean hasProxy() {
        return !getRecipeCapabilitiesProxy().isEmpty();
    }
}
