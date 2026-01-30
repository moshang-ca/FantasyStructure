package org.moshang.fantasystructure.networking.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;

public class ControllerContainerData implements ContainerData {
    private boolean isFormed;
    private ResourceLocation id;

    public ControllerContainerData(boolean isFormed, ResourceLocation id) {
        this.isFormed = isFormed;
        this.id = id;
    }

    public void updateData(boolean formed) {
        this.isFormed = formed;
    }

    @Override
    public int get(int pIndex) {
        return switch (pIndex) {
            case 0 -> this.isFormed ? 1 : 0;
            case 1 -> this.id.hashCode();
            default -> throw new IllegalStateException("Unexpected value: " + pIndex);
        };
    }

    @Override
    public void set(int pIndex, int pValue) {

    }

    @Override
    public int getCount() {
        return 2;
    }

    public boolean isFormed() { return isFormed; }
    public ResourceLocation getId() { return id; }
}
