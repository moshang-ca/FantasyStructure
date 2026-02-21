package org.moshang.fantasystructure.block.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.api.block.BlockItemBusBase;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.blockentity.container.BEItemBus;

public class BlockItemBus extends BlockItemBusBase<BEItemBus> implements EntityBlock {
    public BlockItemBus(int strength, ComponentItemCapacity type, IO io) {
        super(strength, type, io);
    }

    @Override
    protected BEItemBus createBlockEntity(BlockPos pos, BlockState state) {
        return new BEItemBus(pos, state);
    }
}
