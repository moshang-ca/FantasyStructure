package org.moshang.fantasystructure.block.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.api.block.BlockEnergyBusBase;
import org.moshang.fantasystructure.api.capacity.ComponentEnergyCapacity;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.blockentity.container.BEEnergyBus;

public class BlockEnergyBus extends BlockEnergyBusBase<BEEnergyBus> implements EntityBlock {
    public BlockEnergyBus(int strength, ComponentEnergyCapacity type, IO io) {
        super(strength, type, io);
    }

    @Override
    protected BEEnergyBus createBlockEntity(BlockPos pos, BlockState state) {
        return new BEEnergyBus(pos, state);
    }
}
