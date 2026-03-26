package org.moshang.fantasystructure.block.controller;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.block.BlockControllerBase;
import org.moshang.fantasystructure.blockentity.controller.BETestController;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("removal")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockTestController extends BlockControllerBase<BETestController> implements EntityBlock {
    public BlockTestController(int strength) {
        super(
                strength,
                FSBlockEntities.TEST_CONTROLLER_BE,
                () -> new ResourceLocation(FantasyStructure.MODID, "test_structure")
        );
    }

    @Override
    protected BETestController createBlockEntity(BlockPos pos, BlockState state) {
        return new BETestController(getBlockEntityTypeSupplier().get(), pos, state, getPatternIdSupplier().get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if(be instanceof BETestController controller) {
                controller.serverTick();
            }
        };
    }
}
