package org.moshang.fantasystructure.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.slot.ExtendedSlotItemHandler;

public abstract class BaseMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Block targetBlock;

    public BaseMenu(@Nullable MenuType<?> pMenuType, int pContainerId, BlockPos pos, Block targetBlock) {
        super(pMenuType, pContainerId);
        this.pos = pos;
        this.targetBlock = targetBlock;
    }

    public int addSlotLine(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for(int i = 0; i < amount; i++) {
            addSlot(new ExtendedSlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    public int addSlotBox(IItemHandler handler, int index, int x, int y, int dx, int dy, int xAmount, int yAmount) {
        for(int j = 0; j < yAmount; j++) {
            index = addSlotLine(handler, index, x, y, xAmount, dy);
            y += dy;
        }
        return index;
    }

    public void addPlayerInventory(Inventory playerInv, final int x, final int y) {
        LazyOptional<IItemHandler> handler = playerInv.player.getCapability(ForgeCapabilities.ITEM_HANDLER);
        handler.ifPresent( itemHandler -> {
            int currentY = y;
            addSlotBox(itemHandler, 9, x, currentY, 18, 18, 9, 3);
            currentY += 58;
            addSlotLine(itemHandler, 0, x, currentY, 9, 18);
        });
    }

    @Override
    public boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        ItemStack toMove = stack.copy();
        toMove.setCount(Math.min(stack.getMaxStackSize(), stack.getCount()));

        boolean moved = false;

        if(reverseDirection) {
            int tmp = startIndex;
            startIndex = endIndex;
            endIndex = tmp;
        }

        for(int i = startIndex; i < endIndex && !toMove.isEmpty(); i++) {
            Slot targetSlot = this.slots.get(i);
            ItemStack targetStack = targetSlot.getItem();

            if(ItemStack.isSameItemSameTags(toMove, targetStack)) {
                int space = targetSlot.getMaxStackSize(targetStack) - targetStack.getCount();
                if(space > 0) {
                    int transfer =  Math.min(space, toMove.getCount());
                    targetStack.grow(transfer);
                    toMove.shrink(transfer);
                    stack.shrink(transfer);
                    targetSlot.setChanged();
                    moved = true;
                }
            }
        }

        for(int i = startIndex; i < endIndex && !toMove.isEmpty(); i++) {
            Slot targetSlot = this.slots.get(i);
            if(targetSlot.getItem().isEmpty() && targetSlot.mayPlace(toMove)) {
                int transfer = Math.min(targetSlot.getMaxStackSize(toMove), toMove.getCount());
                ItemStack newStack = toMove.copy();
                newStack.setCount(transfer);
                targetSlot.set(newStack);
                toMove.shrink(transfer);
                stack.shrink(transfer);
                targetSlot.setChanged();
                moved = true;
            }
        }

        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public BlockPos getPos() { return pos; }
}
