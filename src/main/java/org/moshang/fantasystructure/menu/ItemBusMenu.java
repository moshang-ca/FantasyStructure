package org.moshang.fantasystructure.menu;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.block.BlockItemBusBase;
import org.moshang.fantasystructure.api.blockentity.BlockEntityItemBusBase;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.slot.ExtendedItemStackHandler;
import org.moshang.fantasystructure.registry.FSMenuType;
import org.slf4j.Logger;

public class ItemBusMenu extends BaseMenu {
    @Getter
    private final ComponentItemCapacity componentCaps;
    private final int slotCount;

    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemBusMenu(int pContainerId, Inventory playInv, FriendlyByteBuf buf) {
        this(
                FSMenuType.ITEM_BUS_MENU_TYPE.get(),
                pContainerId, playInv,
                buf.readBlockPos(),
                buf.readEnum(ComponentItemCapacity.class)
        );
    }

    private ItemBusMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory playInv, BlockPos pos, ComponentItemCapacity pComponentCaps) {
        super(pMenuType, pContainerId, pos);
        this.componentCaps = pComponentCaps;
        this.slotCount = pComponentCaps.getSlots();

        int x = this.componentCaps.getX();
        int y = this.componentCaps.getY();
        int xAmount = this.componentCaps.getXAmount();
        int yAmount = this.componentCaps.getYAmount();

        IItemHandler handler;
        if(playInv.player.level().getBlockEntity(pos) instanceof BlockEntityItemBusBase be) {
            handler = be.getItemHandler();
        } else {                     // This is for safe.
            handler = new ExtendedItemStackHandler(slotCount, componentCaps.getMaxStackSize());
        }
        addSlotBox(handler, 0, x, y, 18, 18, xAmount, yAmount);
        addPlayerInventory(playInv, 8, 140);
    }

    public static ItemBusMenu createForServer(int pContainerId, Inventory playerInv, BlockEntity blockEntity) {
        if(blockEntity instanceof BlockEntityItemBusBase be) {
            return new ItemBusMenu(
                    FSMenuType.ITEM_BUS_MENU_TYPE.get(),
                    pContainerId, playerInv, be.getBlockPos(),
                    be.getBlockState().getValue(BlockItemBusBase.TYPE)
            );
        }
        return null;
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if(pIndex < slotCount) {
                if(!this.moveItemStackTo(itemStack1, slotCount, slotCount + 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(pIndex < slotCount + 36) {
                if(!this.moveItemStackTo(itemStack1, 0, slotCount, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if(itemStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if(itemStack.getCount() == itemStack1.getCount()) return ItemStack.EMPTY;
            slot.onTake(pPlayer, itemStack1);
        }
        return itemStack;
    }
}
