package org.moshang.fantasystructure.api.capability.recipe;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.recipe.FSRecipe;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface IRecipeHandler<K> {
    List<K> handleRecipeInner(IO io, FSRecipe recipe, List<K> left, @Nullable String slotName, boolean simulate);
    ISubscription addChangedListener(Runnable listener);

    default Set<String> getSlotNames() {
        return Collections.emptySet();
    }

    default boolean isDistinct() {
        return false;
    }

    RecipeCapability<K> getRecipeCapability();

    @SuppressWarnings("unchecked")
    default K copyContent(Object content) {
        return getRecipeCapability().copyInner((K) content);
    }

    default List<K> handleRecipe(IO io, FSRecipe recipe, List<?> left, @Nullable String slotName, boolean simulate) {
        return handleRecipeInner(io, recipe, left.stream().map(this::copyContent).collect(Collectors.toList()), slotName, simulate);
    }
}
