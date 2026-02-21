package org.moshang.fantasystructure.capability.recipe;

import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.content.SerializerInteger;

public class EnergyRecipeCapability extends RecipeCapability<Integer> {
    public static final EnergyRecipeCapability INSTANCE = new EnergyRecipeCapability();

    private EnergyRecipeCapability() {
        super("energy", SerializerInteger.INSTANCE);
    }
}
