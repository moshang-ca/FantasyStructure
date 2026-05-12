package org.moshang.fantasystructure.block.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.moshang.fantasystructure.api.block.BlockAbstractBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capacity.ComponentEnergyCapacity;
import org.moshang.fantasystructure.blockentity.container.BEEnergyBus;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockEnergyBus extends BlockAbstractBus<BEEnergyBus> implements EntityBlock {
    public static final EnumProperty<ComponentEnergyCapacity> TYPE = EnumProperty.create("type", ComponentEnergyCapacity.class);

    public BlockEnergyBus(int strength, ComponentEnergyCapacity type, IO io) {
        super(strength, io);
        this.registerDefaultState(this.defaultBlockState().setValue(TYPE, type));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(TYPE);
    }

    @Override
    protected BEEnergyBus createBlockEntity(BlockPos pPos, BlockState pState) {
        return new BEEnergyBus(pPos, pState);
    }
}
