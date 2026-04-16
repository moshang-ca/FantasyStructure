package org.moshang.fantasystructure.block.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;
import org.moshang.fantasystructure.api.block.BlockAbstractBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.blockentity.container.BEItemBus;
import org.moshang.fantasystructure.menu.ItemBusMenu;
import org.moshang.fantasystructure.menu.menuprovider.BlockMenuProvider;

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

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if(be instanceof BEItemBus) {
                BlockState state = be.getBlockState();
                NetworkHooks.openScreen(
                        serverPlayer,
                        new BlockMenuProvider(be, ItemBusMenu.class),
                        buf -> {
                            buf.writeBlockPos(pPos);
                            buf.writeEnum(state.getValue(TYPE));
                        }
                );
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
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
