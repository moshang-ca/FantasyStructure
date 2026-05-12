package org.moshang.fantasystructure.block.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.api.block.BlockAbstractBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capacity.ComponentFluidCapacity;
import org.moshang.fantasystructure.blockentity.container.BEFluidBus;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockFluidBus extends BlockAbstractBus<BEFluidBus> implements EntityBlock {
    public static final EnumProperty<ComponentFluidCapacity> TYPE = EnumProperty.create("type", ComponentFluidCapacity.class);

    public BlockFluidBus(int strength, ComponentFluidCapacity type, IO io) {
        super(strength, io);
        this.registerDefaultState(this.defaultBlockState().setValue(TYPE, type));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE);
    }

    @Override
    @NotNull
    protected BEFluidBus createBlockEntity(BlockPos pPos, BlockState pState) {
        return new BEFluidBus(pPos, pState);
    }
}
