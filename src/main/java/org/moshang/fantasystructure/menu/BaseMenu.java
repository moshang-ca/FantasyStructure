package org.moshang.fantasystructure.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Block targetBlock;

    public BaseMenu(@Nullable MenuType<?> pMenuType, int pContainerId, BlockPos pos, Block targetBlock) {
        super(pMenuType, pContainerId);
        this.pos = pos;
        this.targetBlock = targetBlock;
    }

    public int addSlotLine(Container container, int index, int x, int y, int amount, int dx) {
        for(int i = 0; i < amount; i++) {
            addSlot(new Slot(container, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    public int addSlotBox(Container container, int index, int x, int y, int dx, int dy, int xAmount, int yAmount) {
        for(int j = 0; j < yAmount; j++) {
            index = addSlotLine(container, index, x, y, xAmount, dy);
            y += dy;
        }
        return index;
    }

    public void addPlayerInventory(Container playerInv, int x, int y) {
        addSlotBox(playerInv, 9, x, y, 18, 18, 9, 3);
        y += 58;
        addSlotLine(playerInv, 0, x, y, 9, 18);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public BlockPos getPos() { return pos; }
}
