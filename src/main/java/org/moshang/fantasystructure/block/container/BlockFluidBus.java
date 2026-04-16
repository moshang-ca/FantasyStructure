package org.moshang.fantasystructure.block.container;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.api.block.BlockAbstractBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capacity.ComponentFluidCapacity;
import org.moshang.fantasystructure.blockentity.container.BEFluidBus;
import org.moshang.fantasystructure.menu.FluidBusMenu;
import org.moshang.fantasystructure.menu.menuprovider.BlockMenuProvider;

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
    @SuppressWarnings("deprecation")
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if(be instanceof BEFluidBus) {
                NetworkHooks.openScreen(
                        serverPlayer,
                        new BlockMenuProvider(be, FluidBusMenu.class),
                        buf -> buf.writeBlockPos(pPos)
                );
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    @Override
    @NotNull
    protected BEFluidBus createBlockEntity(BlockPos pPos, BlockState pState) {
        return new BEFluidBus(pPos, pState);
    }
}
