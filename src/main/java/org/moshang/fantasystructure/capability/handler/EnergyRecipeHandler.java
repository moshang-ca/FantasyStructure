package org.moshang.fantasystructure.capability.handler;

import net.minecraftforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.capability.recipe.EnergyRecipeCapability;

import java.util.List;

public class EnergyRecipeHandler extends RecipeHandler<Integer> {
    private final EnergyStorage energyStorage;

    public EnergyRecipeHandler(IO io, EnergyStorage energyStorage) {
        super(io, EnergyRecipeCapability.INSTANCE);
        this.energyStorage = energyStorage;
    }

    @Override
    public List<Integer> handleRecipeInner(IO io, FSRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
        int required = left.stream().reduce(0, Integer::sum);
        var storage = simulate ? createSnapShot() : energyStorage;
        if(io == IO.IN) {
            var extracted = storage.extractEnergy(required, simulate);
            required -= extracted;
        } else {
            var received = storage.receiveEnergy(required, simulate);
            required -= received;
        }
        return required > 0 ? List.of(required) : null;
    }

    private EnergyStorage createSnapShot() {
        int capacity = energyStorage.getMaxEnergyStored();
        return new EnergyStorage(capacity, capacity, capacity, energyStorage.getEnergyStored());
    }
}
