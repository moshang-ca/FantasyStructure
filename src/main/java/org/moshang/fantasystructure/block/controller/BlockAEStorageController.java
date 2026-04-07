package org.moshang.fantasystructure.block.controller;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.block.BlockControllerBase;
import org.moshang.fantasystructure.blockentity.controller.BEAEStorageController;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockAEStorageController extends BlockControllerBase<BEAEStorageController> {
    public BlockAEStorageController(int strength) {
        super(strength,
                FSBlockEntities.AE_STORAGE_CONTROLLER_BE,
                () -> FantasyStructure.id("ae_storage_controller"));
    }

    @Override
    protected BEAEStorageController createBlockEntity(BlockPos pos, BlockState state) {
        return new BEAEStorageController(getBlockEntityTypeSupplier().get(), pos, state, getControllerIdSupplier().get());
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        BlockEntity be = level.getBlockEntity(pos);
        if(be instanceof BEAEStorageController controller && controller.getStructureId() != null) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putUUID("structure_id", controller.getStructureId());
            stack.setTag(tag);
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if(!pLevel.isClientSide) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if(be instanceof BEAEStorageController controller) {
                CompoundTag tag = pStack.getTag();
                if(tag != null && tag.hasUUID("structure_id")) {
                    controller.setStructureId(tag.getUUID("structure_id"));
                } else {
                    controller.setStructureId(UUID.randomUUID());
                    controller.setChanged();
                }
                controller.loadStorageData();
            }
        }
    }
}
