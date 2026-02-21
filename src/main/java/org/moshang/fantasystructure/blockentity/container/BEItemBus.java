package org.moshang.fantasystructure.blockentity.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.api.blockentity.BlockEntityItemBusBase;
import org.moshang.fantasystructure.registry.FSBlockEntities;

public class BEItemBus extends BlockEntityItemBusBase {
    public BEItemBus(BlockPos pPos, BlockState pBlockState) {
        this(
                FSBlockEntities.ITEM_BUS_BE.get(),
                pPos, pBlockState
        );
    }

    public BEItemBus(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }
}
