package org.moshang.fantasystructure.blockentity.container;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidTransferHelperImpl;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.SoundActions;
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
import org.slf4j.Logger;

import java.util.function.Predicate;

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

    @Getter @Persisted @DescSynced
    private final ExtendedFluidTank fluidTank;
    private final LazyOptional<IFluidHandler> handler;

    // TODO: merge fluidTank and filterHandler, reduce the size of the nbt in single block
    @Getter @Persisted @DescSynced
    private final ExtendedFluidTank filterHandler;
    // Not be exposed to outer mod, so unnecessarily declare a lazyOptional.

    @Getter
    private final IRecipeHandler<FluidIngredient> recipeHandler;
    @Getter
    private final RecipeCapability<FluidIngredient> recipeCapability;
    @Getter
    private final IO io;

    private static final Logger LOGGER = LogUtils.getLogger();

    public BEFluidBus(BlockEntityType<?> entityType, BlockPos pos, BlockState state) {
        super(entityType, pos, state);
        var type = state.getValue(BlockFluidBus.TYPE);
        this.fluidTank = createHandler(type.getTanks(), type.getMaxCapacity());
        this.handler = LazyOptional.of(() -> FluidTransferHelperImpl.toFluidHandler(fluidTank));
        this.filterHandler = createHandler(type.getTanks(), 1);

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

    public void fillTank(ServerPlayer player, int tank, ItemStack heldItem) {
        if(level != null && !level.isClientSide) {
            heldItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(itemHandler -> {
                FluidStack available = FluidHelperImpl.toFluidStack(itemHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE));
                if (available.isEmpty()) return;

                if (fluidTank.isFluidValid(tank, available)) {
                    long canFill = fluidTank.fill(tank, available, true, false);
                    if (canFill > 0) {
                        var drained = FluidHelperImpl.toFluidStack(itemHandler.drain((int) canFill, IFluidHandler.FluidAction.EXECUTE));
                        if (!drained.isEmpty()) {
                            fluidTank.fill(tank, drained, false, true);
                            level.playSound(null, player.getX(), player.getY() + .5f, player.getZ(), getSound(drained.getFluid(), false), SoundSource.BLOCKS, 1.F, 1.F);
                            var updatedStack = itemHandler.getContainer();
                            player.containerMenu.setCarried(updatedStack);
                        }
                    }
                }
            });
        }
    }

    public void drainTank(ServerPlayer player, int tank, ItemStack heldItem) {
        if(level != null && !level.isClientSide) {
            FluidStack tankStack = fluidTank.getFluidInTank(tank);
            if (tankStack.isEmpty()) return;

            heldItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(itemHandler -> {
                FluidStack toFill = tankStack.copy();
                toFill.setAmount((int) Math.min(tankStack.getAmount(), 1000));

                int filled = itemHandler.fill(FluidHelperImpl.toFluidStack(toFill), IFluidHandler.FluidAction.SIMULATE);
                if (filled <= 0) return;

                FluidStack drained = fluidTank.drain(tank, FluidStack.create(toFill.getFluid(), filled), false, true);
                if (!drained.isEmpty()) {
                    itemHandler.fill(FluidHelperImpl.toFluidStack(drained), IFluidHandler.FluidAction.EXECUTE);
                    level.playSound(null, player.getX(), player.getY() + .5f, player.getZ(), getSound(drained.getFluid(), true), SoundSource.BLOCKS, 1.F, 1.F);
                    // Maybe this is not a good implementation, but it does work in vanilla.
                    var updatedStack = itemHandler.getContainer();
                    player.containerMenu.setCarried(updatedStack);
                }
            });
        }
    }

    // This is for wildcard or tag filter, but may need to change the design to use this.
    public void setValidatorInTank(int tank, Predicate<FluidStack> validator) {
        fluidTank.setValidatorInTank(tank, validator);
    }

    public void setValidatorInTank(int tank, Fluid fluid) {
        this.filterHandler.setFluidInTank(tank, FluidStack.create(fluid, 1));
        if(fluid != Fluids.EMPTY) {
            fluidTank.setFilter(tank, fluid);
        } else {
            fluidTank.clearFilter(tank);
        }
    }

    public void setValidatorInTank(int tank, FluidStack fluid) {
        setValidatorInTank(tank, fluid.getFluid());
    }

    private SoundEvent getSound(Fluid fluid, boolean isFill) {
        return fluid.getFluidType().getSound(isFill ? SoundActions.BUCKET_FILL : SoundActions.BUCKET_EMPTY);
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
