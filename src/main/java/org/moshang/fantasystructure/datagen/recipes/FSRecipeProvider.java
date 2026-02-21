package org.moshang.fantasystructure.datagen.recipes;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.recipe.FSRecipeBuilder;
import org.moshang.fantasystructure.capability.recipe.EnergyRecipeCapability;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

import java.util.function.Consumer;

public class FSRecipeProvider extends RecipeProvider {
    public FSRecipeProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> finishedRecipe) {
        FSRecipeBuilder.of(FantasyStructure.id("lava_bucket"), FSRecipes.RECIPE_TYPES.get("stellar_simulacrum"))
                .inputItems(Items.BUCKET)
                .perTick(true).input(EnergyRecipeCapability.INSTANCE, 100)
                .duration(20)
                .perTick(false).outputItems(Items.LAVA_BUCKET)
                .save(finishedRecipe);
    }
}
