package org.moshang.fantasystructure.api.blockentity;

import net.minecraft.core.BlockPos;

public interface IStructureComponent {
    void onStructureFormed(BlockPos controllerPos);
    void onStructureDeformed();
}
