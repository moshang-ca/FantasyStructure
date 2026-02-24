package org.moshang.fantasystructure.blockentity.container;

import com.lowdragmc.lowdraglib.side.fluid.forge.FluidTransferHelperImpl;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;
import org.moshang.fantasystructure.api.slot.ExtendedFluidTank;
import org.moshang.fantasystructure.block.container.BlockFluidBus;
import org.moshang.fantasystructure.capability.handler.FluidRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;
import org.moshang.fantasystructure.registry.FSBlockEntities;

public class BEFluidBus extends BlockEntity implements IBus {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BEFluidBus.class);
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }

    @Override
    public void onChanged() {
        setChanged();
    }

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Getter @Persisted
    private final ExtendedFluidTank fluidTank;
    private final LazyOptional<IFluidHandler> handler;

    @Getter
    private final IRecipeHandler<FluidIngredient> recipeHandler;
    @Getter
    private final RecipeCapability<FluidIngredient> recipeCapability;
    @Getter
    private final IO io;

    public BEFluidBus(BlockEntityType<?> entityType, BlockPos pos, BlockState state) {
        super(entityType, pos, state);
        var type = state.getValue(BlockFluidBus.TYPE);
        this.fluidTank = createHandler(type.getTanks(), type.getMaxCapacity());
        this.handler = LazyOptional.of(() -> FluidTransferHelperImpl.toFluidHandler(fluidTank));

        // For Recipe
        this.io = state.getValue(BlockFluidBus.IO_TYPE);
        this.recipeHandler = new FluidRecipeHandler(io, fluidTank);
        this.recipeCapability = FluidRecipeCapability.INSTANCE;
    }

    public BEFluidBus(BlockPos pos, BlockState state) {
        this(FSBlockEntities.FLUID_BUS_BE.get(), pos, state);
    }

    private ExtendedFluidTank createHandler(int tanks, long capacity) {
        return ExtendedFluidTank.create(tanks, capacity, this::setChanged);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.FLUID_HANDLER) {
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
