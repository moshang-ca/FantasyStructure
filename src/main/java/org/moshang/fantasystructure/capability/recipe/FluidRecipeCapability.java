package org.moshang.fantasystructure.capability.recipe;

import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.content.SerializerFluidIngredient;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;

public class FluidRecipeCapability extends RecipeCapability<FluidIngredient> {
    public static final FluidRecipeCapability INSTANCE = new FluidRecipeCapability();

    private FluidRecipeCapability() {
        super("fluid", SerializerFluidIngredient.INSTANCE);
    }
}
