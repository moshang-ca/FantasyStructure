package org.moshang.fantasystructure.api.blockentity;

import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;

public interface IBus {
    IO getIo();
    RecipeCapability<?> getRecipeCapability();
    IRecipeHandler<?> getRecipeHandler();
}
