package org.moshang.fantasystructure.menu.menuprovider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.menu.ControllerMenu;

public class ControllerMenuProvider implements MenuProvider {
    BlockEntity be;
    Block targetBlock;

    public ControllerMenuProvider(BlockEntity be, Block targetBlock) {
        this.be = be;
        this.targetBlock = targetBlock;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.controller");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return ControllerMenu.createForServer(pContainerId, pPlayerInventory, be, targetBlock);
    }
}
