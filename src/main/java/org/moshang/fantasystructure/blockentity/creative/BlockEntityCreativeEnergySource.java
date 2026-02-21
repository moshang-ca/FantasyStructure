package org.moshang.fantasystructure.blockentity.creative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.registry.FSBlockEntities;

public class BlockEntityCreativeEnergySource extends BlockEntity {
    private static final int MAX_OUTPUT = Integer.MAX_VALUE;

    private final CreativeEnergyStorage energyStorage = new CreativeEnergyStorage();

    public BlockEntityCreativeEnergySource(BlockPos pPos, BlockState pBlockState) {
        super(FSBlockEntities.CREATIVE_ENERGY_SOURCE_BE.get(), pPos, pBlockState);
    }

    public void tick(Level level, BlockEntityCreativeEnergySource be) {
        if(level.isClientSide) return;
        be.distribute();
    }

    private void distribute() {
        if(level == null) return;

        for(Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if(neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(receiver -> {
                if(receiver.canReceive()) {
                    int maxReceive = receiver.receiveEnergy(MAX_OUTPUT, true);
                    if(maxReceive > 0) {
                        receiver.receiveEnergy(maxReceive, false);
                    }
                }
            });
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ENERGY) {
            return LazyOptional.of(() -> energyStorage).cast();
        }
        return super.getCapability(cap, side);
    }

    static class CreativeEnergyStorage extends EnergyStorage {
        public CreativeEnergyStorage() {
            super(Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return maxExtract;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }
    }
}
