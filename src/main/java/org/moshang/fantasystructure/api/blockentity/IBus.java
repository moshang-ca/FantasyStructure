package org.moshang.fantasystructure.api.blockentity;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;

public interface IBus extends IManaged, IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity {
    IO getIo();
    RecipeCapability<?> getRecipeCapability();
    IRecipeHandler<?> getRecipeHandler();
}
