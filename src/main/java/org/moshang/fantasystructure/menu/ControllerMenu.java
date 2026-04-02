package org.moshang.fantasystructure.menu;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.BlockEntityControllerBase;
import org.moshang.fantasystructure.registry.FSMenuType;

@SuppressWarnings("removal")
public class ControllerMenu extends BaseMenu {
    @Getter
    private final ResourceLocation structureID;
    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;

    // For Server
    private ControllerMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory playerInv, BlockPos pos) {
        super(pMenuType, pContainerId, pos);
        this.data = new SimpleContainerData(1);
        this.level = playerInv.player.level();
        this.pos = pos;

        if(playerInv.player.level().getBlockEntity(pos) instanceof BlockEntityControllerBase be) {
            this.data.set(0, be.isFormed() ? 1 : 0);
            this.structureID = be.getPatternId();
        } else {
            this.data.set(0, 0);
            this.structureID = new ResourceLocation("null_structure");
        }

        addDataSlots(this.data);
    }

    // For remote
    public ControllerMenu(int pContainerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(FSMenuType.CONTROLLER_MENU_TYPE.get(),
                pContainerId, playerInv, buf.readBlockPos());
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int i) {
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
                    controller.getBlockPos()
            );
        return null;
    }

    public boolean isFormed() {
        return this.data.get(0) == 1;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if(level.getBlockEntity(pos) instanceof BlockEntityControllerBase controller) {
            if(controller.isFormed() == this.isFormed()) return;
            this.data.set(0, controller.isFormed() ? 1 : 0);
        }
    }
}
