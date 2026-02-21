package org.moshang.fantasystructure.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.BlockEntityControllerBase;
import org.moshang.fantasystructure.network.data.ControllerContainerData;
import org.moshang.fantasystructure.registry.FSMenuType;

public class ControllerMenu extends BaseMenu {
    @Deprecated(since = "1.0.0")
    private final ControllerContainerData data;
    // New trait from ldlib.
    // private final BlockEntityControllerBase controller;

    public ControllerMenu(int pContainerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(FSMenuType.CONTROLLER_MENU_TYPE.get(),
                pContainerId,
                playerInv,
                buf.readBlockPos(),
                new ControllerContainerData(buf.readBoolean(), buf.readResourceLocation()));
    }

    private ControllerMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory playerInv,
                           BlockPos pos, boolean isFormed, ResourceLocation id) {
        this(pMenuType, pContainerId, playerInv, pos, new ControllerContainerData(isFormed, id));
    }

    private ControllerMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory playerInv, BlockPos pos,
                          ControllerContainerData data) {
        super(pMenuType, pContainerId, pos);
        this.data = data;

        addPlayerInventory(playerInv, 8, 155);
        addDataSlots(data);
    }

    // New trait from ldlib.
    //    private ControllerMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory playerInv,
    //                           BlockEntityControllerBase controller) {
    //        super(pMenuType, pContainerId, controller.getBlockPos());
    //        this.data = null;
    //        this.controller = controller;
    //        addPlayerInventory(playerInv, 8, 155);
    //    }


    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if(i < 27) {
                if(!this.moveItemStackTo(itemStack1, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(i < 36) {
                if(!this.moveItemStackTo(itemStack1, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if(itemStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if(itemStack.getCount() == itemStack1.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemStack1);
        }
        return itemStack;
    }

    public static ControllerMenu createForServer(int pContainerId, Inventory playerInv, BlockEntity blockEntity) {
        if(blockEntity instanceof BlockEntityControllerBase controller)
            return new ControllerMenu(
                    FSMenuType.CONTROLLER_MENU_TYPE.get(),
                    pContainerId, playerInv,
                    controller.getBlockPos(),
                    controller.isFormed(),
                    controller.getId()
            );
        return null;
    }

/*
    public void updateData(boolean formed) {
        data.updateData(formed);
    }
*/

    public boolean isFormed() { return this.data.isFormed(); }
    public ResourceLocation getId() { return this.data.getId(); }
}
