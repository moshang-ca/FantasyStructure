package org.moshang.fantasystructure.menu;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.slot.ExtendedSlotItemHandler;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseMenu extends AbstractContainerMenu {
    private static final Map<Class<?>, MenuFactory<?>> factoryFunctions = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final BlockPos pos;

    @FunctionalInterface
    public interface MenuFactory<T extends BaseMenu> {
        T create(int pContainerId, Inventory pInv, BlockEntity pBlockEntity);
    }

    public static <T extends BaseMenu> void register(Class<T> type, MenuFactory<T> factory) {
        factoryFunctions.put(type, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T extends BaseMenu> T create(Class<T> type, int pContainerId, Inventory pInv, BlockEntity pBlockEntity) {
        MenuFactory<T> factory = (MenuFactory<T>) factoryFunctions.get(type);
        if (factory == null) {
            return null;
        }
        return factory.create(pContainerId, pInv, pBlockEntity);
    }

    public BaseMenu(@Nullable MenuType<?> pMenuType, int pContainerId, BlockPos pos) {
        super(pMenuType, pContainerId);
        this.pos = pos;
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

    // Will be added back after solve the issue of can not save item with count uppermore than it's max stack size
    /*@Override
    public boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean moved = false;

        if(reverseDirection) {
            int tmp = startIndex;
            startIndex = endIndex;
            endIndex = tmp;
        }

        for(int i = startIndex; i < endIndex && !stack.isEmpty(); i++) {
            Slot targetSlot = this.slots.get(i);
            ItemStack targetStack = targetSlot.getItem();

            if(ItemStack.isSameItemSameTags(stack, targetStack)) {
                int space = targetSlot.getMaxStackSize(targetStack) - targetStack.getCount();
                if(space > 0) {
                    int transfer =  Math.min(space, stack.getCount());
                    targetStack.grow(transfer);
                    stack.shrink(transfer);
                    targetSlot.setChanged();
                    return true;
                }
            }
        }

        for(int i = startIndex; i < endIndex && !stack.isEmpty(); i++) {
            Slot targetSlot = this.slots.get(i);
            if(targetSlot.getItem().isEmpty() && targetSlot.mayPlace(stack)) {
                int transfer = Math.min(targetSlot.getMaxStackSize(stack), stack.getCount());
                ItemStack newStack = stack.copy();
                newStack.setCount(transfer);
                targetSlot.set(newStack);
                stack.shrink(transfer);
                targetSlot.setChanged();
                return true;
            }
        }

        return false;
    }*/

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public BlockPos getPos() { return pos; }
}
