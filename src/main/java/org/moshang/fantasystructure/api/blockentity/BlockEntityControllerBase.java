package org.moshang.fantasystructure.api.blockentity;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.api.recipe.RecipeLogic;
import org.moshang.fantasystructure.api.recipe.content.ContentModifier;
import org.moshang.fantasystructure.data.StructureState;
import org.moshang.fantasystructure.data.save.StructureWorldSavedData;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.helper.builder.StructureBuilderManager;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockEntityControllerBase extends BlockEntity implements IMachine, IEnhancedManaged, IRPCBlockEntity, IAutoSyncBlockEntity, IAutoPersistBlockEntity {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BlockEntityControllerBase.class);
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
    private StructurePattern pattern;
    private CompletableFuture<StructurePattern> patternFuture;
    @Getter @DescSynced
    private final FSStructureDefinitions.StructureDefinition definition;
    private StructureState structureState;

    @Getter
    private final long offset = FantasyStructure.RND.nextLong();
    @Persisted
    private final RecipeLogic recipeLogic;
    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> recipeCapabilityProxies;
    private final List<ISubscription> busSubscriptions = new ArrayList<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings({"UnstableApiUsage"})
    public BlockEntityControllerBase(BlockEntityType<?> entityType,
                                     BlockPos pos, BlockState state, ResourceLocation controllerId) {
        super(entityType, pos, state);
        this.recipeLogic = createRecipeLogic();
        this.definition = FSStructureDefinitions.DEFINITIONS.get(controllerId);
        if(this.definition == null) {
            LOGGER.error("Controller {} has no definition", controllerId);
            FSStructureDefinitions.DEFINITIONS.forEach((definition) -> LOGGER.info("{}: {}", controllerId, definition.patternId()));
        }
        this.recipeCapabilityProxies = Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);
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
        busSubscriptions.forEach(ISubscription::unsubscribe);
        busSubscriptions.clear();

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
                    onDeformed();
                    recipeCapabilityProxies.clear();
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
                    LOGGER.error("Error while getting pattern", e);
                    patternFuture = null;
                    initPattern();
                }
            }
        }
    }

    private void serverTickInternal() {
        if(runRecipeLogic()) {
            recipeLogic.serverTick();
        }
    }

    public boolean runRecipeLogic() {
        return IMachine.super.runRecipeLogic();
    }

    protected void initPattern() {
        if(patternFuture != null && !patternFuture.isDone()) return;
        if (pattern == null && getLevel() != null && !getLevel().isClientSide) {
            var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            this.patternFuture = CompletableFuture.supplyAsync(
                    Util.wrapThreadWithTaskName("init pattern", () -> BlueprintManager.getPattern(definition.patternId(), facing)),
                            Util.backgroundExecutor())
                    .exceptionally(throwable -> {
                        LOGGER.error("Failed load pattern for {}: {}", definition.patternId(), throwable);
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
        resetRecipeCapabilityProxies();
    }

    public void onDeformed() {}

    public void autoBuild(ItemStack builderStack, boolean isCreative) {
        if(pattern == null) initPattern();
        if(level != null && !level.isClientSide) {
            StructureBuilderManager.startBuild(level, worldPosition, this.pattern, builderStack, isCreative);
        }
    }

    @RPCMethod
    private void setFormed(boolean formed) {
        if(this.formed == formed) return;
        this.formed = formed;
        recipeLogic.setStatus(formed);
        setChanged();

        if(level != null && !level.isClientSide) {
            rpcToTracking(this, "setFormed", this.formed);
        }
    }

    @Override
    public @Nullable FSRecipe getModifyRecipe(FSRecipe recipe) {
        recipe = recipe.copy(getMaxParallel(recipe), false);
        return recipe;
    }

    @Override
    @Nullable
    public ContentModifier getMaxParallel(FSRecipe recipe) {
        return new ContentModifier(5, 0);
    }

    protected RecipeLogic createRecipeLogic() {
        return new RecipeLogic(this, formed);
    }

    @Override
    public BlockEntity getHolder() {
        return this;
    }

    @Override
    public Optional<Direction> getFrontFacing() {
        return getBlockState().getOptionalValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public RecipeLogic getRecipeLogic() {
        return recipeLogic;
    }

    @Override
    public Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> getRecipeCapabilitiesProxy() {
        return recipeCapabilityProxies;
    }

    private void resetRecipeCapabilityProxies() {
        recipeCapabilityProxies.clear();
        busSubscriptions.forEach(ISubscription::unsubscribe);
        busSubscriptions.clear();

        for(var bus : structureState.getCollectedBuses()) {
            if(!recipeCapabilityProxies.contains(bus.getIo(), bus.getRecipeCapability())) {
                recipeCapabilityProxies.put(bus.getIo(), bus.getRecipeCapability(), new ArrayList<>());
            }
            //noinspection DataFlowIssue
            recipeCapabilityProxies.get(bus.getIo(), bus.getRecipeCapability()).add(bus.getRecipeHandler());

            var subscription = bus.addContentChangedListener(recipeLogic::wakeUp);
            if(subscription != null) {
                busSubscriptions.add(subscription);
            }
        }
    }

    public StructurePattern getPattern() {
        if(level != null && level.isClientSide) {
            return BlueprintManager.getPattern(definition.patternId(), getFrontFacing().orElse(Direction.NORTH));
        } else {
            return pattern;
        }
    }

    public ResourceLocation getPatternId() {
        return definition.patternId();
    }
}
