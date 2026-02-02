package org.moshang.fantasystructure.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.block.BlockInputBusBase;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.slot.ExtendedItemStackHandler;
import org.moshang.fantasystructure.blockentity.container.BEItemInputBus;
import org.moshang.fantasystructure.registry.FSMenuType;

public class BusMenu extends BaseMenu {
    private final ComponentItemCapacity componentCaps;
    private final int slotCount;

    public BusMenu(@Nullable MenuType<?> pMenuType, int pContainerId, BlockPos pos, Block targetBlock, ComponentItemCapacity pComponentCaps) {
        super(pMenuType, pContainerId, pos, targetBlock);
        this.componentCaps = pComponentCaps;
        this.slotCount = pComponentCaps.getSlots();
    }

    public BusMenu(int pContainerId, Inventory playInv, FriendlyByteBuf buf) {
        this(
                FSMenuType.BUS_MENU_TYPE.get(),
                pContainerId,
                buf.readBlockPos(),
                ForgeRegistries.BLOCKS.getValue(buf.readResourceLocation()),
                buf.readEnum(ComponentItemCapacity.class)
        );

        int x = this.componentCaps.getX();
        int y = this.componentCaps.getY();
        int xAmount = this.componentCaps.getXAmount();
        int yAmount = this.componentCaps.getYAmount();

        addSlotBox(new ExtendedItemStackHandler(slotCount, componentCaps.getMaxStackSize()), 0, x, y, 18, 18, xAmount, yAmount);
        addPlayerInventory(playInv, 8, 140);
    }

    public static BusMenu createForServer(int pContainerId, Inventory playerInv, BlockEntity blockEntity,
                                          int x, int y, int xAmount, int yAmount) {
        if(blockEntity instanceof BEItemInputBus be) {
            BusMenu menu = new BusMenu(
                    FSMenuType.BUS_MENU_TYPE.get(),
                    pContainerId, be.getBlockPos(),
                    be.getBlockState().getBlock(),
                    be.getBlockState().getValue(BlockInputBusBase.TYPE)
            );

            System.out.printf("xAmount: %d, yAmount: %d\n", xAmount, yAmount);

            menu.addSlotBox(be.getItemHandler(), 0, x, y, 18, 18, xAmount, yAmount);
            menu.addPlayerInventory(playerInv, 8, 140);
            return menu;
        }
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if(pIndex < slotCount) {
                if(!this.moveItemStackTo(itemStack1, slotCount, slotCount + 35, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(pIndex < slotCount + 35) {
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

    public ComponentItemCapacity getComponentCaps() {
        return componentCaps;
    }
}
