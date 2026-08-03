package org.moshang.fantasystructure.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BERecipeControllerKjs extends BlockEntityAbstractRecipeController {

    public BERecipeControllerKjs(BlockPos pos, BlockState state) {
        super(null, pos, state,
                ResourceLocation.parse("fantasystructure:unconfigured"),
                1, -1, 1);
    }

    public BERecipeControllerKjs(BlockEntityType<?> entityType, BlockPos pos, BlockState state,
                                 ResourceLocation controllerId, int baseParallel, int parallelLimit, int maxThreads) {
        super(entityType, pos, state, controllerId, baseParallel, parallelLimit, maxThreads);
    }
}
