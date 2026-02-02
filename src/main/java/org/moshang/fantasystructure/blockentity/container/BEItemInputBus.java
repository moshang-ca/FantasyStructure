package org.moshang.fantasystructure.blockentity.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.api.blockentity.BlockEntityInputBusBase;
import org.moshang.fantasystructure.registry.FSBlockEntities;

public class BEItemInputBus extends BlockEntityInputBusBase {
    public BEItemInputBus(BlockPos pPos, BlockState pBlockState) {
        this(
                FSBlockEntities.ITEM_INPUT_BUS_BE.get(),
                pPos, pBlockState
        );
    }

    public BEItemInputBus(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }
}
