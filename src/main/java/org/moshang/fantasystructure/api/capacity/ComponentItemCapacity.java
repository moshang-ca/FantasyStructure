package org.moshang.fantasystructure.api.capacity;

import net.minecraft.util.StringRepresentable;

public enum ComponentItemCapacity implements StringRepresentable {
    TINY(4, 64, 71, 48, 2, 2, "textures/gui/tiny_item_input_bus.png"),
    SMALL(9, 64, 62, 39, 3, 3,"textures/gui/small_item_input_bus.png"),
    MEDIUM(27, 64, 70, 16, 2, 2,"textures/gui/medium_item_input_bus.png"),
    LARGE(54, 64, 8, 18, 9, 6,"textures/gui/genric_item_input_bus.png"),
    GREAT(54, 128, 8, 18, 9, 6,"textures/gui/genric_item_input_bus.png"),
    GIANT(54, 256, 8, 18, 9, 6,"textures/gui/genric_item_input_bus.png"),
    COLOSSAL(54, 1024, 8, 18, 9, 6,"textures/gui/genric_item_input_bus.png"),
    TITANIC(54, 4096, 8, 18, 9, 6,"textures/gui/genric_item_input_bus.png"),
    ENDLESS(540, Integer.MAX_VALUE, 8, 18, 9, 6,"textures/gui/endless_item_input_bus.png");


    private final int slots;
    private final int maxStackSize;
    private final int x, y, xAmount, yAmount;
    private final String guiTexture;

    ComponentItemCapacity(int size, int maxStackSize, int x, int y, int xAmount, int yAmount, String guiTexture) {
        this.slots = size;
        this.maxStackSize = maxStackSize;
        this.x = x;
        this.y = y;
        this.xAmount = xAmount;
        this.yAmount = yAmount;
        this.guiTexture = guiTexture;
    }

    public int getSlots() {
        return slots;
    }
    public int getMaxStackSize() {
        return maxStackSize;
    }
    public String getGuiTexture() { return guiTexture; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getXAmount() { return xAmount; }
    public int getYAmount() { return yAmount; }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
