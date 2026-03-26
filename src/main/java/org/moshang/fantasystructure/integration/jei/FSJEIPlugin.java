package org.moshang.fantasystructure.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

import javax.annotation.ParametersAreNonnullByDefault;

@JeiPlugin
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FSJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return FantasyStructure.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        FantasyStructure.LOGGER.info("JEI register categories");
        var jeiHelpers = registration.getJeiHelpers();
        for(var recipeType : FSRecipes.RECIPE_TYPES.values()) {
            registration.addRecipeCategories(new FSRecipeTypeCategory(jeiHelpers, recipeType));
        }
        registration.addRecipeCategories(new FSStructureInfoCategory(jeiHelpers));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        FSRecipeTypeCategory.registerRecipeCatalysts(registration);
        FSStructureInfoCategory.registerCatalysts(registration);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        FSRecipeTypeCategory.registerRecipes(registration);
        FSStructureInfoCategory.registerRecipes(registration);
    }
}
