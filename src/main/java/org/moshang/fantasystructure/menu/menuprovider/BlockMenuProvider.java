package org.moshang.fantasystructure.menu.menuprovider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.menu.BaseMenu;

public class BlockMenuProvider implements MenuProvider {
    BlockEntity be;
    Class<? extends BaseMenu> menuClass;

    public BlockMenuProvider(BlockEntity be, Class<? extends BaseMenu> menuClass) {
        this.be = be;
        this.menuClass = menuClass;
    }

    @Override
    public Component getDisplayName() {
        return be.getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return BaseMenu.create(menuClass, pContainerId, pPlayerInventory, be);
    }
}
