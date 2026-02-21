package org.moshang.fantasystructure.network.data;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;

@Getter
public class ControllerContainerData implements ContainerData {
    private boolean isFormed;
    private ResourceLocation id;

    public ControllerContainerData(boolean isFormed, ResourceLocation id) {
        this.isFormed = isFormed;
        this.id = id;
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

}
