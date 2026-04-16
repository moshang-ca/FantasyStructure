package org.moshang.fantasystructure.block.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.moshang.fantasystructure.api.block.BlockAbstractBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capacity.ComponentEnergyCapacity;
import org.moshang.fantasystructure.blockentity.container.BEEnergyBus;
import org.moshang.fantasystructure.menu.EnergyBusMenu;
import org.moshang.fantasystructure.menu.menuprovider.BlockMenuProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("deprecation")
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
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if(be instanceof BEEnergyBus) {
                NetworkHooks.openScreen(
                        serverPlayer,
                        new BlockMenuProvider(be, EnergyBusMenu.class),
                        buf -> buf.writeBlockPos(pPos)
                );
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    @Override
    protected BEEnergyBus createBlockEntity(BlockPos pPos, BlockState pState) {
        return new BEEnergyBus(pPos, pState);
    }
}
