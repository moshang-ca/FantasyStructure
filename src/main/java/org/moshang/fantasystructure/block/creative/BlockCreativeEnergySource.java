package org.moshang.fantasystructure.block.creative;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.blockentity.creative.BlockEntityCreativeEnergySource;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockCreativeEnergySource extends Block implements EntityBlock {
    public BlockCreativeEnergySource(int strength) {
        super(Properties.of()
                .strength(strength)
                .requiresCorrectToolForDrops());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BlockEntityCreativeEnergySource(pPos, pState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if(pLevel.isClientSide) return null;

        return (level, pos, state, be) -> {
            if(be instanceof BlockEntityCreativeEnergySource source) {
                source.tick(level, source);
            }
        };
    }
}
