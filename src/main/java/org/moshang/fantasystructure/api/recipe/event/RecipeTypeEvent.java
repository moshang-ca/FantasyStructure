package org.moshang.fantasystructure.api.recipe.event;

import net.minecraftforge.eventbus.api.Event;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;

public class RecipeTypeEvent extends Event {
    protected final FSRecipeType recipeType;

    public RecipeTypeEvent(FSRecipeType recipeType) {
        this.recipeType = recipeType;
    }
}
