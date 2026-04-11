package org.moshang.fantasystructure.blockentity.controller;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.orientation.BlockOrientation;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.blockentity.BlockEntityAbstractController;
import org.moshang.fantasystructure.integration.ae2.node.StorageNodeListener;
import org.moshang.fantasystructure.integration.ae2.storage.StorageData;
import org.moshang.fantasystructure.integration.ae2.storage.StorageDataManager;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class BEAEStorageController extends BlockEntityAbstractController implements IStorageProvider, IGridConnectedBlockEntity {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(BEAEStorageController.class, BlockEntityAbstractController.MANAGED_FIELD_HOLDER);
    protected final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }

    private final IManagedGridNode mainNode;

    @Getter @Setter @Persisted @DropSaved @DescSynced
    private UUID structureId;
    @DescSynced
    private StorageData storageData;
    @Getter @Setter
    private boolean isMounted = false;

    public BEAEStorageController(BlockEntityType<?> entityType, BlockPos pos, BlockState state, ResourceLocation controllerId) {
        super(entityType, pos, state, controllerId);
        this.upgradeInv.setFilter(stack -> stack.getItem() instanceof IBasicCellItem);
        this.mainNode = GridHelper.createManagedNode(this, StorageNodeListener.INSTANCE)
                .setVisualRepresentation(getBlockState().getBlock().asItem())
                .setIdlePowerUsage(1.f)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setInWorldNode(true)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .addService(IStorageProvider.class, this);
    }

    public BEAEStorageController(BlockPos pos, BlockState state) {
        this(FSBlockEntities.AE_STORAGE_CONTROLLER_BE.get(),
                pos, state,
                FantasyStructure.id("ae_storage_controller"));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(level != null && !level.isClientSide) {
            if(mainNode.getNode() == null) {
                mainNode.create(level, worldPosition);
            }

            if(structureId != null) {
                loadStorageData();
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if(level != null && !level.isClientSide) {
            mainNode.destroy();

            if(structureId != null) {
                StorageDataManager.unload(structureId);
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();

        if(level != null && !level.isClientSide) {
            mainNode.destroy();

            if(structureId != null) {
                StorageDataManager.unload(structureId);
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);
        mainNode.saveToNBT(pTag);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        mainNode.loadFromNBT(pTag);
    }

    @Override
    public void onUpgrade() {
        for(int i = 0; i < this.upgradeInv.getSlots(); ++i) {
            if(addCapacity(this.upgradeInv.getStackInSlot(i))) {
                this.upgradeInv.extractItem(i, 1, false);
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    protected WidgetGroup createUI() {
        var root = super.createUI();

        var infoWidget = root.getFirstWidgetById("info_widget");
        ((WidgetGroup) infoWidget).addWidget(new LabelWidget(5, 5, () -> LocalizationUtils.format(
                "fantasystructure.gui.structure_info.storage",
                storageData.getUsedBytes(), storageData.getMaxBytes(),
                storageData.getUsedTypes(), storageData.getMaxTypes())));
        return root;
    }

    @Override
    public void mountInventories(IStorageMounts iStorageMounts) {
        if(storageData != null && !isMounted) {
            iStorageMounts.mount(storageData, IStorageMounts.DEFAULT_PRIORITY);
            isMounted = true;
        }
    }

    @Override
    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    @Override
    public void saveChanges() {
        if (level != null && !level.isClientSide && structureId != null) {
            StorageDataManager.save(structureId);
        }
        setChanged();
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return IGridConnectedBlockEntity.super.getGridConnectableSides(orientation);
    }

    @Override
    public void onFormed() {
        super.onFormed();
        if(level != null && !level.isClientSide) {
            loadStorageData();
        }
    }

    @Override
    public void onDeformed(boolean isRemoved) {
        super.onDeformed(isRemoved);
        if(level != null && !level.isClientSide) {
            if(structureId != null) {
                StorageDataManager.unload(structureId);
                storageData = null;
                isMounted = false;
            }
            updateStorage();
        }
    }

    public boolean addCapacity(ItemStack cellItem) {
        if(level == null || level.isClientSide
                || storageData == null || cellItem.isEmpty()) return false;

        var identifier = CellIdentifier.fromItemStack(cellItem);
        if(identifier == null) return false;

        storageData.addCapacity(identifier.types, identifier.bytes);
        setChanged();
        updateStorage();
        return true;
    }

    public void loadStorageData() {
        if(structureId == null) return;
        this.storageData = StorageDataManager.getOrLoad(structureId);
    }

    public void updateStorage() {
        if(level == null || level.isClientSide) return;

        if(getMainNode().isActive()) {
            IStorageProvider.requestUpdate(getMainNode());
        }

        isMounted = false;
    }

    public record CellIdentifier(long types, long bytes) {
        @Nullable
            public static CellIdentifier fromItemStack(ItemStack cellItem) {
                if (cellItem.getItem() instanceof IBasicCellItem cell) {
                    return new CellIdentifier((long) cell.getTotalTypes(cellItem) << 1, (long) cell.getBytes(cellItem) << 1);
                }
                return null;
            }
        }
}
