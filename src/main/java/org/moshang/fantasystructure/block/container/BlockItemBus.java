package org.moshang.fantasystructure.block.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.moshang.fantasystructure.api.block.BlockAbstractBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.blockentity.container.BEItemBus;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockItemBus extends BlockAbstractBus<BEItemBus> implements EntityBlock {
    public static final EnumProperty<ComponentItemCapacity> TYPE = EnumProperty.create("type", ComponentItemCapacity.class);

    public BlockItemBus(int strength, ComponentItemCapacity type, IO io) {
        super(strength, io);
        this.registerDefaultState(this.defaultBlockState().setValue(TYPE, type));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(TYPE);
    }

    protected void dropContainerItems(Level level, BlockPos pos) {
        if(!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if(be != null) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for(int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if(!stack.isEmpty()) {
                            popResource(level, pos, handler.getStackInSlot(i));
                        }
                    }
                });
            }
        }
    }

    @Override
    protected BEItemBus createBlockEntity(BlockPos pPos, BlockState pState) {
        return new BEItemBus(pPos, pState);
    }
}
