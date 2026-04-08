package org.moshang.fantasystructure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.blockentity.BEAEConnector;

public class BlockAEConnector extends Block implements EntityBlock {
    public BlockAEConnector(int strength) {
        super(Properties.of()
                .strength(strength)
                .requiresCorrectToolForDrops());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new BEAEConnector(pPos, pState);
    }
}
