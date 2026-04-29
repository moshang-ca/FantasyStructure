package org.moshang.fantasystructure.api.blockentity;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeCapabilityHolder;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.api.recipe.MultiRecipeThread;

import java.util.Optional;

/**
 * Any controller must implement this to have capability of handling recipes
 */
public interface IRecipeMachine extends IRecipeCapabilityHolder {
    BlockEntity getHolder();
    Optional<Direction> getFrontFacing();
    FSRecipeType getRecipeType();
    // @NotNull RecipeLogic getRecipeLogic();
    @NotNull MultiRecipeThread getMultiRecipeThread();
    long getOffset();


    default Level getLevel() {
        return getHolder().getLevel();
    }

    default BlockPos getPos() {
        return getHolder().getBlockPos();
    }

    default BlockState getBlockState() {
        return getHolder().getBlockState();
    }

    default void markDirty() {
        var level = getLevel();
        if(level != null && !level.isClientSide && level.getServer() != null) {
            level.getServer().execute(() -> getHolder().setChanged());
        }
    }

    default boolean isValid() {
        return getHolder().isRemoved();
    }

    default void scheduleRenderUpdate() {
        var pos = getPos();
        var level = getLevel();
        if (level != null) {
            var state = level.getBlockState(pos);
            if (level.isClientSide) {
                level.sendBlockUpdated(pos, state, state, 1 << 3);
            }
        }
    }

    default boolean runRecipeThread() {
        return getRecipeType() != FSRecipeType.DUMMY;
    }

    default long getOffsetTimer() {
        Level level = getLevel();
        return level == null ? getOffset() : (level.getGameTime() + getOffset());
    }

    @Nullable
    default FSRecipe doModifyRecipe(@NotNull FSRecipe recipe) {
        recipe = getModifyRecipe(recipe);
        if(recipe == null) {
            return null;
        }
        return applyParallel(recipe, getMaxParallel());
    }

    @Nullable
    default Pair<FSRecipe, Integer> doModifyRecipe(@NotNull FSRecipe recipe, int maxParallel) {
        recipe = getModifyRecipe(recipe);
        if(recipe == null) {
            return null;
        }
        return FSRecipe.calculateParallel(this, recipe, maxParallel);
    }

    @Nullable
    default FSRecipe getModifyRecipe(@NotNull FSRecipe recipe) {
        return recipe;
    }

    default int getMaxParallel() {
        return 1;
    }

    @NotNull
    default FSRecipe applyParallel(@NotNull FSRecipe recipe, int maxParallel) {
        if(maxParallel > 1) {
            var result = FSRecipe.calculateParallel(this, recipe, maxParallel);
            return result.getFirst();
        }
        return recipe;
    }

//    default boolean beforeWorking(FSRecipe recipe) {
//     return false;
// }
//
// default boolean onWorking() {
//     return false;
// }
//
// default void onWaiting() {
//
// }
//
// default void afterWorking() {
//
// }
//
// default void onRecipeFinish() {
//
// }
}
