package org.moshang.fantasystructure.blockentity;

import appeng.api.config.CpuSelectionMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.blockentity.grid.AENetworkBlockEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.BlockEntityAbstractController;
import org.moshang.fantasystructure.api.blockentity.IStructureComponent;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import java.util.List;

public class BEAEConnector extends AENetworkBlockEntity implements IStructureComponent, IStorageProvider, ICraftingCPU, ICraftingProvider {
    @Getter @Setter
    private BlockPos controllerPos;
    private BlockEntityAbstractController cached;

    public BEAEConnector(BlockPos pos, BlockState blockState) {
        super(FSBlockEntities.AE_CONNECTOR_BE.get(), pos, blockState);
        getMainNode()
                .addService(IStorageProvider.class, this)
                .addService(ICraftingProvider.class, this);
    }

    @Nullable
    public BlockEntity getController() {
        if(controllerPos == null || level == null || !level.isLoaded(controllerPos)) return null;
        return cached != null ? cached : level.getBlockEntity(controllerPos);
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        if(controllerPos != null) {
            data.putLong("controllerPos", controllerPos.asLong());
        }
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if(data.contains("controllerPos")) {
            this.controllerPos = BlockPos.of(data.getLong("controllerPos"));
        }
    }

    // For storage controller
    @Override
    public void mountInventories(IStorageMounts iStorageMounts) {
        if (getController() instanceof IStorageProvider provider) {
            provider.mountInventories(iStorageMounts);
        }
    }

    // For crafting controller
    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        if(getController() instanceof ICraftingProvider provider) {
            return provider.getAvailablePatterns();
        }
        return List.of();
    }

    @Override
    public boolean pushPattern(IPatternDetails iPatternDetails, KeyCounter[] keyCounters) {
        if(getController() instanceof ICraftingProvider provider) {
            return provider.pushPattern(iPatternDetails, keyCounters);
        }
        return false;
    }

    // For crafting cpu controller
    @Override
    public void cancelJob() {
        if(getController() instanceof ICraftingCPU cpu) {
            cpu.cancelJob();
        }
    }

    @Override
    public boolean isBusy() {
        var controller = getController();
        if(controller instanceof ICraftingCPU cpu) {
            return cpu.isBusy();
        } else if(controller instanceof ICraftingProvider provider) {
            return provider.isBusy();
        }
        return false;
    }

    @Override
    public @Nullable CraftingJobStatus getJobStatus() {
        if(getController() instanceof ICraftingCPU cpu) {
            cpu.getJobStatus();
        }
        return null;
    }

    @Override
    public long getAvailableStorage() {
        if(getController() instanceof ICraftingCPU cpu) {
            cpu.getAvailableStorage();
        }
        return 0;
    }

    @Override
    public int getCoProcessors() {
        if(getController() instanceof ICraftingCPU cpu) {
            cpu.getCoProcessors();
        }
        return 0;
    }

    @Override
    public CpuSelectionMode getSelectionMode() {
        if(getController() instanceof ICraftingCPU cpu) {
            cpu.getSelectionMode();
        }
        return CpuSelectionMode.ANY;
    }

    @Override
    public void onStructureDeformed() {
        this.controllerPos = null;
        this.cached = null;
        IStorageProvider.requestUpdate(getMainNode());
        this.setChanged();
    }

    @Override
    public void onStructureFormed(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        if(level != null) {
            this.cached = level.getBlockEntity(controllerPos) instanceof BlockEntityAbstractController controller ? controller : null;
        }
        IStorageProvider.requestUpdate(getMainNode());
        this.setChanged();
    }
}
