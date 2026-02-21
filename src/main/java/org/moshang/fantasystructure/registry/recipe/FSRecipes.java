package org.moshang.fantasystructure.registry.recipe;

import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.FSRecipeSerializer;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.capability.recipe.EnergyRecipeCapability;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;

import java.util.List;

public class FSRecipes {
    public static final FSRecipeRegistry.String<RecipeCapability<?>> RECIPE_CAPABILITIES = new FSRecipeRegistry.String<>(FantasyStructure.id("recipe_capability"));
    public static final FSRecipeRegistry.RL<FSRecipeType> RECIPE_TYPES = new FSRecipeRegistry.RL<>(FantasyStructure.id("recipe_type"));

    //=============== RECIPE TYPE CONSTANCE ===============
    public static final List<FSRecipeType> TYPES = List.of(
            new FSRecipeType(FantasyStructure.id("stellar_simulacrum"))
    );

    public static void initRecipeCapabilities() {
        RECIPE_CAPABILITIES.unfreeze();
        RECIPE_CAPABILITIES.register(ItemRecipeCapability.INSTANCE.getName(), ItemRecipeCapability.INSTANCE);
        RECIPE_CAPABILITIES.register(FluidRecipeCapability.INSTANCE.getName(), FluidRecipeCapability.INSTANCE);
        RECIPE_CAPABILITIES.register(EnergyRecipeCapability.INSTANCE.getName(), EnergyRecipeCapability.INSTANCE);
        RECIPE_CAPABILITIES.freeze();
    }

    public static void initRecipeTypes() {
        RECIPE_TYPES.unfreeze();
        TYPES.forEach(type -> RECIPE_TYPES.register(type.getRegistryName(), type));
        RECIPE_TYPES.freeze();
        RECIPE_TYPES.forEach(k -> {
            ForgeRegistries.RECIPE_TYPES.register(k.getRegistryName(), k);
            var serializer = new FSRecipeSerializer();
            ForgeRegistries.RECIPE_SERIALIZERS.register(k.getRegistryName(), FSRecipeSerializer.SERIALIZER);
        });

    }
}
