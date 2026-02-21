package org.moshang.fantasystructure.capability.recipe;

import net.minecraft.world.item.crafting.Ingredient;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.content.SerializerIngredient;

public class ItemRecipeCapability extends RecipeCapability<Ingredient> {
    public static final ItemRecipeCapability INSTANCE = new ItemRecipeCapability();

    private ItemRecipeCapability() {
        super("item", SerializerIngredient.INSTANCE);
    }
}
