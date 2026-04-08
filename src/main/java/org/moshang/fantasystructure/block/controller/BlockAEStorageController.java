package org.moshang.fantasystructure.block.controller;

import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.block.BlockAbstractController;
import org.moshang.fantasystructure.blockentity.controller.BEAEStorageController;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockAEStorageController extends BlockAbstractController<BEAEStorageController> {
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
        if(be instanceof IAutoPersistBlockEntity dropSaved) {
            var tag = stack.getOrCreateTag();
            dropSaved.saveManagedPersistentData(tag, true);
        }
        return stack;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pParams) {
        var opt = Optional.ofNullable(pParams.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
        if(opt.isPresent() && opt.get() instanceof IAutoPersistBlockEntity dropSaved) {
            var drop = new ItemStack(this);
            var tag = drop.getOrCreateTag();
            dropSaved.saveManagedPersistentData(tag, true);
            return List.of(drop);
        }
        return super.getDrops(pState, pParams);
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if(!pLevel.isClientSide) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if(be instanceof BEAEStorageController controller) {
                CompoundTag tag = pStack.getTag();
                if(tag != null) {
                    controller.loadManagedPersistentData(tag);
                } else {
                    controller.setStructureId(UUID.randomUUID());
                    controller.setChanged();
                }
                controller.loadStorageData();
            }
        }
    }
}
