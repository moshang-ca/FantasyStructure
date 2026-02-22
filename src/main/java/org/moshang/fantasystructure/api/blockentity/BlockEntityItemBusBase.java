package org.moshang.fantasystructure.api.blockentity;

import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.block.BlockItemBusBase;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.slot.ExtendedItemStackHandler;
import org.moshang.fantasystructure.capability.handler.ItemSlotRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;

public abstract class BlockEntityItemBusBase extends BlockEntity implements IBus {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BlockEntityItemBusBase.class);
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Override
    public void onChanged() {
        setChanged();
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    private final ComponentItemCapacity type;

    @Getter @Persisted
    private final ExtendedItemStackHandler itemHandler;
    private final LazyOptional<IItemHandler> handler;
    @Getter
    private final IRecipeHandler<Ingredient> recipeHandler;
    @Getter
    private final IO io;
    @Getter
    private final RecipeCapability<Ingredient> recipeCapability;

    public BlockEntityItemBusBase(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.type = pBlockState.getValue(BlockItemBusBase.TYPE);
        this.itemHandler = createHandler(type.getSlots(), type.getMaxStackSize());
        this.handler = LazyOptional.of(() -> itemHandler);

        // For Recipe
        this.io = pBlockState.getValue(BlockItemBusBase.IO_TYPE);
        this.recipeHandler = new ItemSlotRecipeHandler(io, itemHandler);
        this.recipeCapability = ItemRecipeCapability.INSTANCE;
    }

    private ExtendedItemStackHandler createHandler(int size, int maxStackSize) {
        return new ExtendedItemStackHandler(size, maxStackSize) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }



//    @Override
//    protected void saveAdditional(CompoundTag tag) {
//        super.saveAdditional(tag);
//        tag.put("ItemHandler", itemHandler.serializeNBT());
//    }
//
//    @Override
//    public void load(CompoundTag tag) {
//        super.load(tag);
//        if(tag.contains("ItemHandler")) {
//            itemHandler.deserializeNBT(tag.getCompound("ItemHandler"));
//        }
//    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }
}
