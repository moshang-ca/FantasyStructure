package org.moshang.fantasystructure.api.recipe.event;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.Cancelable;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.api.recipe.FSRecipeBuilder;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;

@Getter
@Cancelable
public class TransferProxyRecipeEvent extends RecipeTypeEvent {
    private final ResourceLocation proxyTypeID;
    private final RecipeType<?> proxyType;
    private final ResourceLocation proxyRecipeID;
    private final Recipe<?> proxyRecipe;
    private FSRecipe fsRecipe;

    public TransferProxyRecipeEvent(FSRecipeType recipeType, ResourceLocation proxyTypeID, RecipeType<?> proxyType,
                                    ResourceLocation proxyRecipeID, Recipe<?> proxyRecipe, FSRecipe fsRecipe) {
        super(recipeType);
        this.proxyTypeID = proxyTypeID;
        this.proxyType = proxyType;
        this.proxyRecipeID = proxyRecipeID;
        this.proxyRecipe = proxyRecipe;
        this.fsRecipe = fsRecipe;
    }

    public FSRecipeBuilder recipeBuilder() {
        return recipeType.getBuilder();
    }
}
