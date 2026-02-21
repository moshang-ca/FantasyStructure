package org.moshang.fantasystructure.api.capability.recipe;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.recipe.FSRecipe;

import java.util.List;
import java.util.Set;

public record RecipeHandlerSlotsProxy<T>(IRecipeHandler<T> proxy, Set<String> slotNames) implements IRecipeHandler<T> {
    @Override
    public List<T> handleRecipeInner(IO io, FSRecipe recipe, List<T> left, @Nullable String slotName, boolean simulate) {
        return proxy.handleRecipeInner(io, recipe, left, slotName, simulate);
    }

    @Override
    public ISubscription addChangedListener(Runnable listener) {
        return null;
    }

    @Override
    public Set<String> getSlotNames() {
        return slotNames;
    }

    @Override
    public boolean isDistinct() {
        return proxy.isDistinct();
    }

    @Override
    public RecipeCapability<T> getRecipeCapability() {
        return proxy.getRecipeCapability();
    }

    @Override
    public T copyContent(Object content) {
        return proxy.copyContent(content);
    }

    @Override
    public List<T> handleRecipe(IO io, FSRecipe recipe, List<?> left, @Nullable String slotName, boolean simulate) {
        return proxy.handleRecipe(io, recipe, left, slotName, simulate);
    }
}
