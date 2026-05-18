package org.moshang.fantasystructure.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.StartupEventJS;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

@SuppressWarnings("unused")
public class FSRecipeTypeRegistryEventJS extends StartupEventJS {
    public FSRecipeType createRecipeType(String id) {
        var rlId = ResourceLocation.parse(id);
        FSRecipeType recipeType = new FSRecipeType(rlId);
        FSRecipes.RECIPE_TYPES.register(rlId, recipeType);
        return recipeType;
    }

    @Nullable
    public FSRecipeType getRecipeType(ResourceLocation id) {
        return FSRecipes.RECIPE_TYPES.get(id);
    }

    public void removeRecipeType(ResourceLocation id) {
        FSRecipes.RECIPE_TYPES.remove(id);
    }
}
