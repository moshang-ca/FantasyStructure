package org.moshang.fantasystructure.api.capacity;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ComponentEnergyCapacity implements StringRepresentable {
    TINY(10_000, 100, 300, "textures/gui/tiny_energy_input_bus.png"),
    SMALL(50_000, 500, 1500, "textures/gui/small_energy_input_bus.png"),
    MEDIUM(250_000, 2_500, 25_000, "textures/gui/medium_energy_input_bus.png"),
    LARGE(1_000_000, 10_000, 100_000, "textures/gui/large_energy_input_bus.png"),
    GREAT(5_000_000, 50_000, 500_000, "textures/gui/green_energy_input_bus.png"),
    GIANT(25_000_000, 250_000, 2_500_000, "textures/gui/green_energy_input_bus.png"),
    COLOSSAL(100_000_000, 1_000_000, 10_000_000, "textures/gui/colossal_energy_input_bus.png"),
    TITANIC(500_000_000, 5_000_000, 50_000_000, "textures/gui/titanic_energy_input_bus.png"),
    ENDLESS(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, "textures/gui/endless_energy_input_bus.png");


    private final int maxCapacity;
    private final int maxReceiveCap;
    private final int maxExtractCap;
    private final String guiTexture;

    ComponentEnergyCapacity(int maxCapacity, int maxReceiveCap, int maxExtractCap, String guiTexture) {
        this.maxCapacity = maxCapacity;
        this.maxReceiveCap = maxReceiveCap;
        this.maxExtractCap = maxExtractCap;
        this.guiTexture = guiTexture;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
