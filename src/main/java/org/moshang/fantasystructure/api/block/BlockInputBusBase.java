package org.moshang.fantasystructure.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.BlockEntityInputBusBase;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.menu.menuprovider.BusMenuProvider;

public abstract class BlockInputBusBase<T extends BlockEntityInputBusBase> extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ComponentItemCapacity> TYPE = EnumProperty.create("type", ComponentItemCapacity.class);


    public BlockInputBusBase(int strength, ComponentItemCapacity type) {
        super(Block.Properties.of()
                .strength(strength)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TYPE, type)
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        try {
            return createBlockEntity(pPos, pState);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create block entity", e);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
        pBuilder.add(TYPE);
    }

    @javax.annotation.Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        direction = context.getPlayer().isShiftKeyDown() ? direction.getOpposite() : direction;
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if(be instanceof BlockEntityInputBusBase) {
                BlockState state = be.getBlockState();
                NetworkHooks.openScreen(
                        serverPlayer,
                        new BusMenuProvider(
                                state.getValue(TYPE).getX(),
                                state.getValue(TYPE).getY(),
                                state.getValue(TYPE).getXAmount(),
                                state.getValue(TYPE).getYAmount(),
                                be
                        ),
                        buf -> {
                                buf.writeBlockPos(pPos);
                                buf.writeResourceLocation(ForgeRegistries.BLOCKS.getKey(this));
                                buf.writeEnum(state.getValue(TYPE));
                        }
                );
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    protected abstract T createBlockEntity(BlockPos pos, BlockState state);
}
