package org.moshang.fantasystructure.api.slot;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.Tag;
import net.minecraftforge.energy.EnergyStorage;

public class SyncedEnergyStorage extends EnergyStorage implements ITagSerializable<Tag>, IContentChangeAware {
    @Getter @Setter
    private Runnable onContentsChanged = () -> {};


    public SyncedEnergyStorage(int capacity) {
        super(capacity);
    }

    public SyncedEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public SyncedEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    public SyncedEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        var extracted = super.extractEnergy(maxExtract, simulate);
        if(extracted > 0) onContentsChanged.run();
        return extracted;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        var received = super.receiveEnergy(maxReceive, simulate);
        if(received > 0) onContentsChanged.run();
        return received;
    }
}
