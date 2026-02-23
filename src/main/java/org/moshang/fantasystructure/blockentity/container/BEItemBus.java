package org.moshang.fantasystructure.blockentity.container;

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
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.slot.ExtendedItemStackHandler;
import org.moshang.fantasystructure.block.container.BlockItemBus;
import org.moshang.fantasystructure.capability.handler.ItemSlotRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;
import org.moshang.fantasystructure.registry.FSBlockEntities;

public class BEItemBus extends BlockEntity implements IBus {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BEItemBus.class);
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

    @Getter @Persisted
    private final ExtendedItemStackHandler itemHandler;
    private final LazyOptional<IItemHandler> handler;
    @Getter
    private final IRecipeHandler<Ingredient> recipeHandler;
    @Getter
    private final IO io;
    @Getter
    private final RecipeCapability<Ingredient> recipeCapability;

    public BEItemBus(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        ComponentItemCapacity type = pBlockState.getValue(BlockItemBus.TYPE);
        this.itemHandler = createHandler(type.getSlots());
        this.handler = LazyOptional.of(() -> itemHandler);

        // For Recipe
        this.io = pBlockState.getValue(BlockItemBus.IO_TYPE);
        this.recipeHandler = new ItemSlotRecipeHandler(io, itemHandler);
        this.recipeCapability = ItemRecipeCapability.INSTANCE;
    }

    public BEItemBus(BlockPos pPos, BlockState pBlockState) {
        this(
                FSBlockEntities.ITEM_BUS_BE.get(),
                pPos, pBlockState
        );
    }

    private ExtendedItemStackHandler createHandler(int size) {
        return new ExtendedItemStackHandler(size) {
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
