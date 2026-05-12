package org.moshang.fantasystructure.api.block;

import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockAbstractBus<T extends BlockEntity & IBus> extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<IO> IO_TYPE = EnumProperty.create("io_type", IO.class);

    public BlockAbstractBus(int strength, IO io) {
        super(BlockBehaviour.Properties.of()
                .strength(strength)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(IO_TYPE, io));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity( BlockPos pPos, BlockState pState) {
        try {
            return createBlockEntity(pPos, pState);
        }catch (Exception e) {
            throw new RuntimeException("Failed to create block entity", e);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(IO_TYPE, FACING);
    }

    @Override
    public @Nullable PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        direction = context.getPlayer().isShiftKeyDown() ? direction.getOpposite() : direction;
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        dropContainerItems(pLevel, pPos);
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if(be instanceof IUIHolder) {
                BlockEntityUIFactory.INSTANCE.openUI(be, serverPlayer);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    protected abstract T createBlockEntity(BlockPos pPos, BlockState pState);
    protected void dropContainerItems(Level level, BlockPos pos) {}
}
