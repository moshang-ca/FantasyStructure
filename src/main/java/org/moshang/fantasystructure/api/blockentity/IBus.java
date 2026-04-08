package org.moshang.fantasystructure.api.blockentity;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import net.minecraft.core.BlockPos;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;

/**
 * An interface which used to mark a block entity as a bus.
 * All block entity implement this will be regarded as a bus.
 * */
public interface IBus extends IStructureComponent, IManaged, IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity {
    IO getIo();
    RecipeCapability<?> getRecipeCapability();
    IRecipeHandler<?> getRecipeHandler();

    default ISubscription addContentChangedListener(Runnable listener) {
        return null;
    }

    @Override
    default void onStructureDeformed() {}

    @Override
    default void onStructureFormed(BlockPos controllerPos) {}
}
