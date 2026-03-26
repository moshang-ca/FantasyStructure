package org.moshang.fantasystructure.datagen.recipes;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.recipe.FSRecipeBuilder;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

import java.util.function.Consumer;

public class FSRecipeProvider extends RecipeProvider {
    public FSRecipeProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> finishedRecipe) {
        FSRecipeBuilder.of(FantasyStructure.id("coal_energy"), FSRecipes.RECIPE_TYPES.get("stellar_simulacrum"))
                .inputFluids(FluidStack.create(Fluids.WATER, 1000))
                .outputFluids(FluidStack.create(Fluids.LAVA, 1000))
                .duration(200)
                .save(finishedRecipe);
    }
}
