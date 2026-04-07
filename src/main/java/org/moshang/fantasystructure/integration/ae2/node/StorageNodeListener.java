package org.moshang.fantasystructure.integration.ae2.node;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import org.moshang.fantasystructure.blockentity.controller.BEAEStorageController;

public class StorageNodeListener implements IGridNodeListener<BEAEStorageController> {
    public static final StorageNodeListener INSTANCE = new StorageNodeListener();

    private StorageNodeListener() {}

    @Override
    public void onSaveChanges(BEAEStorageController controller, IGridNode node) {
        controller.saveChanges();
    }

    @Override
    public void onGridChanged(BEAEStorageController nodeOwner, IGridNode node) {
        nodeOwner.setChanged();
    }

    @Override
    public void onStateChanged(BEAEStorageController nodeOwner, IGridNode node, State state) {
        if (state == State.GRID_BOOT) {
            nodeOwner.setMounted(false);
            nodeOwner.updateStorage();
        }
        nodeOwner.setChanged();
    }
}
