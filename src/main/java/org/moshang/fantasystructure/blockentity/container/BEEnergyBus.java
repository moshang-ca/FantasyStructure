package org.moshang.fantasystructure.blockentity.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.api.blockentity.BlockEntityEnergyBusBase;
import org.moshang.fantasystructure.registry.FSBlockEntities;

public class BEEnergyBus extends BlockEntityEnergyBusBase {
    public BEEnergyBus(BlockPos pos, BlockState state) {
        this(
                FSBlockEntities.ENERGY_BUS_BE.get(),
                pos, state
        );
    }

    public BEEnergyBus(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }
}
