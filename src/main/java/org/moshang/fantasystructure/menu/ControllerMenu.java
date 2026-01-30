package org.moshang.fantasystructure.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.BlockEntityControllerBase;
import org.moshang.fantasystructure.networking.data.ControllerContainerData;
import org.moshang.fantasystructure.registry.FSMenuType;

public class ControllerMenu extends BaseMenu{
    private final ControllerContainerData data;
    private final Block targetBlock;

    public ControllerMenu(int pContainerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(FSMenuType.CONTROLLER_MENU_TYPE.get(),
                pContainerId,
                buf.readBlockPos(),
                new ControllerContainerData(buf.readBoolean(), buf.readResourceLocation()),
                buf.readResourceLocation());

        addPlayerInventory(playerInv, 8, 155);
        addDataSlots(data);
    }

    public ControllerMenu(@Nullable MenuType<?> pMenuType, int pContainerId, BlockPos pos,
                          ControllerContainerData data, ResourceLocation blockId) {
        super(pMenuType, pContainerId, pos, null);
        this.data = data;
        this.targetBlock = ForgeRegistries.BLOCKS.getValue(blockId);

        addDataSlots(data);
    }

    public ControllerMenu(@Nullable MenuType<?> pMenuType, int pContainerId, BlockPos pos,
                          Block targetBlock, boolean isFormed, ResourceLocation id) {
        super(pMenuType, pContainerId, pos, null);
        this.data = new ControllerContainerData(isFormed, id);
        this.targetBlock = targetBlock;
    }

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

    public static ControllerMenu createForServer(int pContainerId, Inventory playerInv, BlockEntity blockEntity, Block targetBlock) {
        if(blockEntity instanceof BlockEntityControllerBase controller) {
            ControllerMenu menu = new ControllerMenu(
                    FSMenuType.CONTROLLER_MENU_TYPE.get(),
                    pContainerId,
                    controller.getBlockPos(),
                    targetBlock,
                    controller.getFormed(),
                    controller.getId()
            );
            menu.addPlayerInventory(playerInv, 8, 155);
            return menu;
        }
        return null;
    }

    public void updateData(boolean formed) {
        data.updateData(formed);
    }

    public boolean isFormed() { return this.data.isFormed(); }
    public ResourceLocation getId() { return this.data.getId(); }
}
