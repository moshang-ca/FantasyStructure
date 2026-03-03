package org.moshang.fantasystructure.api.slot;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.syncdata.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Getter
@SuppressWarnings({"unchecked", "UnstableApiUsage"})
public class ExtendedFluidTank implements IFluidTransfer, IManaged {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ExtendedFluidTank.class);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        onContentChanged.run();
    }

    private final int tanks;
    @Persisted @DescSynced
    private final FluidStack[] stacks;
    @Persisted
    private final long[] capacities;
    private final Predicate<FluidStack>[] validators;
    @Persisted @DescSynced
    private final List<Fluid> filters;
    private Runnable onContentChanged;

    private ExtendedFluidTank(int tanks, long[] capacities, Predicate<FluidStack>[] validators, Runnable onContentChanged) {
        this.tanks = tanks;
        this.stacks = Stream.generate(FluidStack::empty).limit(tanks).toArray(FluidStack[]::new);
        this.capacities = capacities;
        this.validators = validators;
        this.filters = new ArrayList<>(Collections.nCopies(tanks, Fluids.EMPTY));
        this.onContentChanged = onContentChanged;
    }

    private ExtendedFluidTank(ExtendedFluidTank other) {
        this.tanks = other.tanks;
        this.stacks = new FluidStack[other.tanks];
        for(int i = 0; i < this.tanks; i++) {
            this.stacks[i] = other.stacks[i].isEmpty() ? FluidStack.empty() : other.stacks[i].copy();
        }
        this.capacities = other.capacities.clone();
        this.validators = other.validators.clone();
        this.filters = new ArrayList<>(other.filters);
        this.onContentChanged = other.onContentChanged;
    }

    public static ExtendedFluidTank create(int tanks, long capacity) {
        return create(tanks, capacity, () -> {});
    }

    public static ExtendedFluidTank create(int tanks, long capacity, Runnable onContentsChanged) {
        long[] capacities = LongStream.generate(() -> capacity).limit(tanks).toArray();
        return create(tanks, capacities, onContentsChanged);
    }

    public static ExtendedFluidTank create(int tanks, long[] capacities, Runnable onContentsChanged) {
        Predicate<FluidStack>[] validators = Stream.generate(() -> (Predicate<FluidStack>) fluid -> true)
                .limit(tanks).toArray(Predicate[]::new);
        return new ExtendedFluidTank(tanks, capacities, validators, onContentsChanged);
    }

    @SuppressWarnings("UnusedReturnValue")
    public ExtendedFluidTank setValidatorInTank(int tank, Predicate<FluidStack> validator) {
        this.validators[tank] = validator;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ExtendedFluidTank setFilter(int tank, Fluid fluid) {
        if (tank >= 0 && tank < tanks) {
            var inStack = getFluidInTank(tank);
            if(inStack.isEmpty() || inStack.getFluid().isSame(fluid)) {
                this.filters.set(tank, fluid);
                setValidatorInTank(tank, stack -> stack.isFluidEqual(FluidStack.create(fluid, 1)));
                onContentsChanged();
            }
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ExtendedFluidTank clearFilter(int tank) {
        if (tank >= 0 && tank < tanks) {
            this.filters.set(tank, Fluids.EMPTY);
            setValidatorInTank(tank, stack -> true);
            onContentsChanged();
        }
        return this;
    }

    public Fluid getFilter(int tank) {
        return this.filters.get(tank);
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return stacks[tank];
    }

    @Override
    public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {
        if(fluidStack.isEmpty()) {
            stacks[tank] = FluidStack.empty();
        } else {
            stacks[tank] = fluidStack.copy();
        }
        onContentsChanged();
    }

    @Override
    public long getTankCapacity(int tank) {
        return capacities[tank];
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return validators[tank].test(stack);
    }

    @Override
    public long fill(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        if(resource.isEmpty() || !isFluidValid(tank, resource)) {
            return 0;
        }
        if(simulate) {
            if(stacks[tank].isEmpty()) {
                return Math.min(resource.getAmount(), capacities[tank]);
            }
            if(!stacks[tank].isFluidEqual(resource)) {
                return 0;
            }
            return Math.min(resource.getAmount(), capacities[tank] - stacks[tank].getAmount());
        }
        if(stacks[tank].isEmpty()) {
            stacks[tank] = FluidStack.create(resource, Math.min(resource.getAmount(), capacities[tank]));
            if(notifyChanges) onContentsChanged();
            return stacks[tank].getAmount();
        }
        if(!stacks[tank].isFluidEqual(resource)) {
            return 0;
        }
        long filled = capacities[tank] - stacks[tank].getAmount();

        if(resource.getAmount() < filled) {
            stacks[tank].grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            stacks[tank].setAmount(capacities[tank]);
        }
        if(filled > 0 && notifyChanges) onContentsChanged();
        return filled;
    }

    @Override
    public boolean supportsFill(int tank) {
        return true;
    }

    @Override
    public @NotNull FluidStack drain(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        if(resource.isEmpty() || stacks[tank].isEmpty() || !isFluidValid(tank, resource)) {
            return FluidStack.empty();
        }

        if(simulate) {
            if(stacks[tank].isFluidEqual(resource)) {
                return FluidStack.create(stacks[tank], Math.min(resource.getAmount(), stacks[tank].getAmount()));
            } else {
                return FluidStack.empty();
            }
        }
        long canDrained = Math.min(resource.getAmount(), stacks[tank].getAmount());
        var result = FluidStack.create(stacks[tank], canDrained);
        if(canDrained > 0) {
            stacks[tank].shrink(canDrained);
            if(stacks[tank].isEmpty()) {
                setFluidInTank(tank, FluidStack.empty());
            }
            if(notifyChanges) onContentsChanged();
        }
        return result;
    }

    @Override
    public boolean supportsDrain(int tank) {
        return true;
    }

    /***
     * Rewrite for forge, the return value must be cast to {@code ExtendedFluidTank} or, basically, {@code IFluidTransfer}
     */
    @Override
    public @NotNull Object createSnapshot() {
        return deepCopy();
    }

    private @NotNull ExtendedFluidTank deepCopy() {
        return new ExtendedFluidTank(this);
    }

    @Override
    public void restoreFromSnapshot(Object snapshot) {

    }

    @Override
    public void onContentsChanged() {
        IFluidTransfer.super.onContentsChanged();
        this.onContentChanged.run();
    }

//    @Override
//    public CompoundTag serializeNBT() {
//        CompoundTag tag = new CompoundTag();
//        ListTag fluids = new ListTag();
//        for (FluidStack stack : stacks) {
//            fluids.add(stack.saveToTag(new CompoundTag()));
//        }
//        tag.put("Fluids", fluids);
//        ListTag filters = new ListTag();
//        for(var fluid : this.filters) {
//            if(fluid != null) {
//                filters.add(StringTag.valueOf(fluid.toString()));
//            } else {
//                filters.add(StringTag.valueOf(""));
//            }
//        }
//        tag.put("Filters", filters);
//        tag.putLongArray("capacities", capacities);
//        return tag;
//    }

//    @Override
//    public void deserializeNBT(CompoundTag nbt) {
//        ListTag listTag;
//        if(nbt.contains("Fluids")) {
//            listTag = nbt.getList("Fluids", 10);
//            for(int i = 0; i < listTag.size(); ++i) {
//                CompoundTag compoundTag = listTag.getCompound(i);
//                this.stacks[i] = FluidStack.loadFromTag(compoundTag);
//            }
//        }
//        if(nbt.contains("Filters")) {
//            listTag = nbt.getList("Filters", 8);
//            // filters.clear();
//            for(int i = 0; i < listTag.size(); ++i) {
//                var fluidName = ResourceLocation.tryParse(listTag.getString(i));
//                System.out.println("fluidName: " + fluidName);
//                if(fluidName != null) {
//                    // filters.add(i, null);
//                    setFilter(i, fluidName);
//                } else {
//                    // filters.add(i, null);
//                    clearFilter(i);
//                }
//            }
//        }
//        if(nbt.contains("Capacities")) {
//            long[] capacities = nbt.getLongArray("Capacities");
//            System.arraycopy(capacities, 0, this.capacities, 0, capacities.length);
//        }
//    }
//
//    @Override
//    public void setOnContentsChanged(Runnable onContentChanged) {
//        this.onContentChanged = onContentChanged;
//    }
//
//    @Override
//    public Runnable getOnContentsChanged() {
//        return onContentChanged;
//    }
}
