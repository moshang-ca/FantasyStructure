package org.moshang.fantasystructure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.blockentity.BlockEntityControllerBase;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class BlockControllerBase<T extends BlockEntityControllerBase> extends Block implements EntityBlock {
    private static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final Supplier<BlockEntityType<T>> blockEntityTypeSupplier;
    private final Supplier<ResourceLocation> patternIdSupplier;

    protected BlockControllerBase(int strength,
                                  Supplier<BlockEntityType<T>> blockEntityTypeSupplier,
                                  Supplier<ResourceLocation> patternIdSupplier) {
        super(Block.Properties.of()
                        .strength(strength)
                        .requiresCorrectToolForDrops()
        );
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
        );
        this.blockEntityTypeSupplier = blockEntityTypeSupplier;
        this.patternIdSupplier = patternIdSupplier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        try {
            return createBlockEntity(pos, state);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create block entity: ", e);
        }
    }

    protected abstract T createBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public  <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> entityType) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if(be instanceof BlockEntityControllerBase controller) {
                controller.tick();
            }
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        direction = context.getPlayer().isShiftKeyDown() ? direction.getOpposite() : direction;
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @NotNull
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @NotNull
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public Supplier<BlockEntityType<T>> getBlockEntityTypeSupplier() {
        return blockEntityTypeSupplier;
    }

    public Supplier<ResourceLocation> getPatternIdSupplier() {
        return patternIdSupplier;
    }
}
