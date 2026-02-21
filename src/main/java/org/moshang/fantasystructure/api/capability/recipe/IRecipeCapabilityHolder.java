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
