package org.moshang.fantasystructure.api.blockentity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.block.BlockEnergyBusBase;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.capacity.ComponentEnergyCapacity;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.capability.handler.EnergyRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.EnergyRecipeCapability;

public abstract class BlockEntityEnergyBusBase extends BlockEntity implements IBus {
    private final EnergyStorage energyStorage;
    private final LazyOptional<IEnergyStorage> energyHandler;

    @Getter
    private final int capacity;
    @Getter
    private final int maxReceive;
    @Getter
    private final int maxExtract;
    @Getter
    private final IO io;
    @Getter
    private final IRecipeHandler<Integer> recipeHandler;
    @Getter
    private final RecipeCapability<Integer> recipeCapability;

    public BlockEntityEnergyBusBase(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        ComponentEnergyCapacity type = pBlockState.getValue(BlockEnergyBusBase.TYPE);
        this.capacity = type.getMaxCapacity();
        this.maxReceive = type.getMaxReceiveCap();
        this.maxExtract = type.getMaxExtractCap();

        this.energyStorage = new EnergyStorage(capacity, maxReceive, maxExtract);
        this.energyHandler = LazyOptional.of(() -> energyStorage);

        // For Recipe
        this.io = pBlockState.getValue(BlockEnergyBusBase.IO_TYPE);
        this.recipeHandler = new EnergyRecipeHandler(io, energyStorage);
        this.recipeCapability = EnergyRecipeCapability.INSTANCE;
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if(pTag.contains("EnergyStorage")) energyStorage.deserializeNBT(pTag.get("EnergyStorage"));
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("EnergyStorage", energyStorage.serializeNBT());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public int getEnergyStored() { return energyStorage.getEnergyStored(); }

    public void setEnergyStorageDebug(int value) {
        this.energyStorage.receiveEnergy(value, false);
    }
}
