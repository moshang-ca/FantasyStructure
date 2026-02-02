package org.moshang.fantasystructure.menu.menuprovider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.menu.BusMenu;

public class BusMenuProvider implements MenuProvider {
    private final int slotX, slotY;
    private final int xAmount, yAmount;
    private final BlockEntity be;

    public BusMenuProvider(int slotX, int slotY, int xAmount, int yAmount, BlockEntity be) {
        this.slotX = slotX;
        this.slotY = slotY;
        this.xAmount = xAmount;
        this.yAmount = yAmount;
        this.be = be;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.bus");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return BusMenu.createForServer(pContainerId, pPlayerInventory, be, slotX, slotY, xAmount, yAmount);
    }
}
