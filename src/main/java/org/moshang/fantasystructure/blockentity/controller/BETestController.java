package org.moshang.fantasystructure.blockentity.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.blockentity.BlockEntityControllerBase;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.block.controller.BlockTestController;
import org.moshang.fantasystructure.registry.FSBlockEntities;
import org.moshang.fantasystructure.registry.FSBlocks;

@SuppressWarnings("removal")
public class BETestController extends BlockEntityControllerBase {
    private static final Vec3i renderCenter = new Vec3i(10, 15, 5);

    public BETestController(BlockPos pos, BlockState state) {
        this(
                FSBlockEntities.TEST_CONTROLLER_BE.get(),
                pos, state,
                new ResourceLocation(FantasyStructure.MODID, "test_controller")
        );
    }

    public BETestController(BlockEntityType<?> entityType, BlockPos pos,
                            BlockState state, ResourceLocation controllerId) {
        super(entityType, pos, state, controllerId);
    }

    @Override
    public FSRecipeType getRecipeType() {
        return getDefinition().recipeType();
    }

    private Vec3i getRenderCenter() {
        var direction = getBlockState().getValue(BlockTestController.FACING).getOpposite();
        if(direction.getStepX() != 0) {
            return new Vec3i(renderCenter.getX() * direction.getStepX(), renderCenter.getY(), renderCenter.getZ());
        } else if(direction.getStepZ() != 0) {
            return new Vec3i(renderCenter.getX(), renderCenter.getY(), renderCenter.getZ() * direction.getStepZ());
        }
        return renderCenter;
    }

    @Override
    public void onFormed() {
        super.onFormed();
        if(level != null && !level.isClientSide) {
            level.setBlock(worldPosition.offset(getRenderCenter()), FSBlocks.STAR_CORE.get().defaultBlockState(), 3);
        }
    }

    @Override
    public void onDeformed() {
        if(level != null && !level.isClientSide) {
            level.removeBlock(worldPosition.offset(getRenderCenter()), false);
        }
    }
}
