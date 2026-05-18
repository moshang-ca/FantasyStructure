package org.moshang.fantasystructure.registry.recipe;

import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.capability.recipe.EnergyRecipeCapability;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;

public class FSRecipes {
    public static final FSRecipeRegistry.String<RecipeCapability<?>> RECIPE_CAPABILITIES = new FSRecipeRegistry.String<>(FantasyStructure.id("recipe_capability"));
    public static final FSRecipeRegistry.RL<FSRecipeType> RECIPE_TYPES = new FSRecipeRegistry.RL<>(FantasyStructure.id("recipe_type"));

    private static final FSRecipeType STELLAR_SIMULACRUM = new FSRecipeType(FantasyStructure.id("stellar_simulacrum"));

    // Register custom recipe type and capability before common setup event, or the registries will be frozen.
    static {
        RECIPE_TYPES.unfreeze();
        RECIPE_TYPES.register(STELLAR_SIMULACRUM.getRegistryName(), STELLAR_SIMULACRUM);

        RECIPE_CAPABILITIES.unfreeze();
        RECIPE_CAPABILITIES.register(ItemRecipeCapability.INSTANCE.getName(), ItemRecipeCapability.INSTANCE);
        RECIPE_CAPABILITIES.register(FluidRecipeCapability.INSTANCE.getName(), FluidRecipeCapability.INSTANCE);
        RECIPE_CAPABILITIES.register(EnergyRecipeCapability.INSTANCE.getName(), EnergyRecipeCapability.INSTANCE);
    }

    private FSRecipes() {}
}
