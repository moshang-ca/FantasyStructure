package org.moshang.fantasystructure.block.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.block.BlockInputBusBase;
import org.moshang.fantasystructure.api.blockentity.BlockEntityInputBusBase;
import org.moshang.fantasystructure.blockentity.container.BEItemInputBus;

public class BlockItemInputBus extends BlockInputBusBase implements EntityBlock {
    public BlockItemInputBus(int strength, ComponentItemCapacity type) {
        super(strength, type);
    }

    @Override
    protected BlockEntityInputBusBase createBlockEntity(BlockPos pos, BlockState state) {
        return new BEItemInputBus(pos, state);
    }
}
