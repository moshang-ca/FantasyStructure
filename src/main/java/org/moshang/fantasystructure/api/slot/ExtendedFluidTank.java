package org.moshang.fantasystructure.api.slot;

import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidStorage;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.syncdata.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Getter
@SuppressWarnings({"unchecked", "UnstableApiUsage", "UnusedReturnValue"})
public class ExtendedFluidTank implements IFluidTransfer, IContentChangeAware, ITagSerializable<CompoundTag> {
    private final int tanks;
    private final FluidStorage[] storages;
    private final FilterStorage[] filters;
    private final long[] capacities;
    @Getter @Setter
    private Runnable onContentsChanged;

    private ExtendedFluidTank(int tanks, long[] capacities, Predicate<FluidStack>[] validators, Runnable onContentsChanged) {
        this.tanks = tanks;
        this.storages = new FluidStorage[tanks];
        this.filters = new FilterStorage[tanks];
        for(int i = 0; i < tanks; i++) {
            this.storages[i] = new FluidStorage(capacities[i], validators[i]);
            this.filters[i] = new FilterStorage(i);
        }
        this.capacities = capacities;
        this.onContentsChanged = onContentsChanged;
    }

    private ExtendedFluidTank(FluidStorage[] storages) {
        this.storages = new FluidStorage[storages.length];
        this.filters = new FilterStorage[storages.length];
        for(int i = 0; i < storages.length; i++) {
            this.storages[i] = ((FluidStorage) storages[i].createSnapshot());
        }
        this.capacities = Stream.of(storages).mapToLong(FluidStorage::getCapacity).toArray();
        this.tanks = storages.length;
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

    protected ExtendedFluidTank setValidatorInTank(int tank, Predicate<FluidStack> validator) {
        this.storages[tank].setValidator(validator);
        if(onContentsChanged != null) onContentsChanged.run();
        return this;
    }

    public ExtendedFluidTank setValidatorInTank(int tank, FluidStack stack) {
        var tarTank = this.storages[tank];
        if(stack == null) {
            this.filters[tank].setFluid(FluidStack.empty());
            return setValidatorInTank(tank, stack1 -> true);
        } else if(!tarTank.getFluid().isEmpty()
                && !tarTank.getFluid().isFluidEqual(stack)) {
            this.filters[tank].rollbackFluid();
            return this;
        }
        this.filters[tank].setFluid(stack);
        return setValidatorInTank(tank, stack1 -> stack1.isFluidEqual(stack));
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return storages[tank].getFluid();
    }

    @Override
    public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {
        storages[tank].setFluid(fluidStack);
        onContentsChanged();
    }

    @Override
    public long getTankCapacity(int tank) {
        return capacities[tank];
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
    }

    @Override
    public long fill(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        return storages[tank].fill(0, resource, simulate, notifyChanges);
    }

    @Override
    public boolean supportsFill(int tank) {
        return storages[tank].supportsFill(tank);
    }

    @Override
    public @NotNull FluidStack drain(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        return storages[tank].drain(0, resource, simulate, notifyChanges);
    }

    @Override
    public boolean supportsDrain(int tank) {
        return storages[tank].supportsDrain(tank);
    }

    /***
     * Rewrite for forge, the return value must be cast to {@code ExtendedFluidTank} or, basically, {@code IFluidTransfer}
     */
    @Override
    public @NotNull Object createSnapshot() {
        return new ExtendedFluidTank(storages);
    }

    @Override
    public void restoreFromSnapshot(Object snapshot) {}

    @Override
    public void onContentsChanged() {
        this.onContentsChanged.run();
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        ListTag storagesTag = new ListTag();
        ListTag filtersTag = new ListTag();
        for(int i = 0; i < tanks; ++i) {
            if(!storages[i].getFluid().isEmpty()) {
                CompoundTag fluidTag = new CompoundTag();
                fluidTag.putInt("s", i);
                fluidTag.put("f", storages[i].serializeNBT());
                storagesTag.add(fluidTag);
            }
            if(!filters[i].isEmpty()) {
                CompoundTag fTag = new CompoundTag();
                fTag.putInt("s", i);
                fTag.put("f", filters[i].serializeNBT());
                filtersTag.add(fTag);
            }
        }
        tag.put("storages", storagesTag);
        tag.put("filters", filtersTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ListTag storagesTag = nbt.getList("storages", 10);
        ListTag filtersTag = nbt.getList("filters", 10);
        for(int i = 0; i < storagesTag.size(); ++i) {
            var tag = storagesTag.getCompound(i);
            this.storages[tag.getInt("s")].deserializeNBT(tag.getCompound("f"));
        }

        for(int i = 0; i < filtersTag.size(); ++i) {
            var tag = filtersTag.getCompound(i);
            int tank = tag.getInt("s");
            this.filters[tank].deserializeNBT(tag.getCompound("f"));
            setValidatorInTank(tank, filters[tank].getFluid());
        }
    }

    private static class FilterStorage implements IFluidStorage, IContentChangeAware, ITagSerializable<CompoundTag> {
        private final int tank;
        @Getter @Setter
        private Runnable onContentsChanged = () -> {};
        @Getter @NotNull
        private FluidStack fluid = FluidStack.empty();
        @Getter
        private FluidStack lastValidFluid = fluid;

        public FilterStorage(int tank) {
            this.tank = tank;
        }

        public boolean isEmpty() {
            return fluid.isEmpty();
        }

        @Override
        public long getCapacity() {
            return 0;
        }

        @Override
        public void setFluid(FluidStack fluid) {
            this.lastValidFluid = this.fluid;
            this.fluid = fluid;
            onContentsChanged();
        }

        public void rollbackFluid() {
            this.fluid = lastValidFluid;
            onContentsChanged();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return true;
        }

        @Override
        public long fill(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
            if(!simulate)
                setFluid(resource);
            return resource.getAmount();
        }

        @Override
        public boolean supportsFill(int tank) {
            return true;
        }

        @Override
        public @NotNull FluidStack drain(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
            return FluidStack.empty();
        }

        @Override
        public boolean supportsDrain(int tank) {
            return true;
        }

        @Override
        public @NotNull Object createSnapshot() {
            return fluid.copy();
        }

        @Override
        public void restoreFromSnapshot(Object snapshot) {}

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            setFluid(FluidStack.loadFromTag(nbt));
        }

        @Override
        public CompoundTag serializeNBT() {
            return fluid.saveToTag(new CompoundTag());
        }

        @Override
        public void onContentsChanged() {
            onContentsChanged.run();
        }
    }
}
