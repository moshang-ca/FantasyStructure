package org.moshang.fantasystructure.block;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.blockentity.BEStarCore;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockStarCore extends Block implements EntityBlock {
    public BlockStarCore() {
        super(Properties.of()
                .noOcclusion()
                .noCollission()
                .lightLevel(state -> 15));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BEStarCore(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.f;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T>
    getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if(pLevel.isClientSide) return null;
        return createTickerHelper(pBlockEntityType, FSBlockEntities.STAR_CORE_BE.get(),
                (lvl, pos, st, be) -> be.tick());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A>
    createTickerHelper(BlockEntityType<A> actualType, BlockEntityType<E> expectedType,
                       BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(pLevel.isClientSide) {
            if(pLevel.getBlockEntity(pPos) instanceof BEStarCore star) {
                pPlayer.sendSystemMessage(Component.literal("rotation angle: %f".formatted(star.getRotationAngle())));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
