package org.moshang.fantasystructure.api.blockentity;

import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.data.StructureState;
import org.moshang.fantasystructure.data.save.StructureWorldSavedData;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.helper.builder.StructureBuilderManager;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class BlockEntityAbstractController extends BlockEntity implements IEnhancedManaged, IRPCBlockEntity, IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BlockEntityAbstractController.class);
    protected final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }

    @Override
    public void onChanged() {
        this.setChanged();
    }

    @Override
    public void scheduleRenderUpdate() {}

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Getter @Persisted @DescSynced
    protected boolean formed = false;
    protected StructurePattern pattern;
    protected CompletableFuture<StructurePattern> patternFuture;
    @Getter @DescSynced
    protected final FSStructureDefinitions.StructureDefinition definition;
    protected StructureState structureState;

    public BlockEntityAbstractController(BlockEntityType<?> entityType,
                                               BlockPos pos, BlockState state, ResourceLocation controllerId) {
        super(entityType, pos, state);
        this.definition = FSStructureDefinitions.DEFINITIONS.get(controllerId);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            var savedData = StructureWorldSavedData.getOrCreate(serverLevel);
            this.structureState = savedData.getStructure(worldPosition);
            if(this.structureState == null) {
                initPattern();
            } else {
                pattern = structureState.getPattern();
            }
        }
    }

    @Override
    public void setRemoved() {
        if(level instanceof ServerLevel serverLevel && structureState != null) {
            var savedData = StructureWorldSavedData.getOrCreate(serverLevel);
            savedData.removeStructure(worldPosition);
        }

        super.setRemoved();
    }

    public void serverTick() {
        if(level == null || level.isClientSide) return;

        if(structureState != null) {
            boolean wasFormed = this.formed;
            boolean isValid = structureState.tickCheck(level);

            if(wasFormed != isValid) {
                setFormed(isValid);
                if(isValid) {
                    onFormed();
                } else {
                    onDeformed(false);
                }
            }
            if(isValid) {
                serverTickInternal();
            }
        } else {
            if(patternFuture != null && patternFuture.isDone()) {
                try {
                    pattern = patternFuture.get();
                    createStructureState();
                } catch (Exception e) {
                    FantasyStructure.LOGGER.error("Error while getting pattern", e);
                    patternFuture = null;
                    initPattern();
                }
            }
        }
    }

    protected void serverTickInternal() {}

    protected void initPattern() {
        if(patternFuture != null && !patternFuture.isDone()) return;
        if (pattern == null && getLevel() != null && !getLevel().isClientSide) {
            var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            this.patternFuture = CompletableFuture.supplyAsync(
                            Util.wrapThreadWithTaskName("init pattern", () -> BlueprintManager.getPattern(definition.patternId(), facing)),
                            Util.backgroundExecutor())
                    .exceptionally(throwable -> {
                        FantasyStructure.LOGGER.error("Failed load pattern for {}: {}", definition.patternId(), throwable);
                        return null;
                    });
        }
    }

    private void createStructureState() {
        if (pattern != null && level instanceof ServerLevel serverLevel) {
            structureState = new StructureState(worldPosition, pattern);
            StructureWorldSavedData savedData = StructureWorldSavedData.getOrCreate(serverLevel);
            savedData.registerStructure(structureState);
            boolean isValid = structureState.tickCheck(level);
            setFormed(isValid);
            if (isValid) {
                onFormed();

            }
        }
    }

//    public void onRotate() {
//        initPattern();
//        setChanged();
//    }

    public void onFormed() {
        structureState.notifyAllComponents(formed);
    }

    public void onDeformed(boolean isRemoved) {
        FantasyStructure.LOGGER.info("Deforming structure");
        structureState.notifyAllComponents(!isRemoved && formed);
    }

    public void autoBuild(ItemStack builderStack, boolean isCreative) {
        if(pattern == null) initPattern();
        if(level != null && !level.isClientSide) {
            StructureBuilderManager.startBuild(level, worldPosition, this.pattern, builderStack, isCreative);
        }
    }

    @RPCMethod
    protected void setFormed(boolean formed) {
        if(this.formed == formed) return;
        this.formed = formed;
        setChanged();

        if(level != null && !level.isClientSide) {
            rpcToTracking(this, "setFormed", this.formed);
        }
    }

    public StructurePattern getPattern() {
        if(level != null && level.isClientSide) {
            return BlueprintManager.getPattern(definition.patternId(),
                    getBlockState().getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH));
        } else {
            return pattern;
        }
    }

    public ResourceLocation getPatternId() {
        return definition.patternId();
    }
}
