package org.moshang.fantasystructure.capability.handler;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.IRecipeMachine;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;

import java.util.*;

public abstract class RecipeHandler<K> implements IRecipeHandler<K> {
    protected final Random random = new Random();
    protected final IO io;
    protected final RecipeCapability<K> capability;
    protected final List<Runnable> listeners = new ArrayList<>();
    protected boolean isDistinct  =false;
    protected Set<String> slotNames = new HashSet<>();

    @Nullable @Getter @Setter
    protected IRecipeMachine machine = null;

    public RecipeHandler(IO io, RecipeCapability<K> capability) {
        this.io = io;
        this.capability = capability;
    }

    @Override
    public ISubscription addChangedListener(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public RecipeCapability<K> getRecipeCapability() {
        return capability;
    }

    @Override
    public Set<String> getSlotNames() {
        return slotNames;
    }

    protected void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
