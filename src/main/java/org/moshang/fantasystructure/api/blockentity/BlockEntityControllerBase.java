package org.moshang.fantasystructure.api.blockentity;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.helper.builder.StructureBuilderManager;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockEntityControllerBase extends BlockEntity implements IMachine, IEnhancedManaged {
    private static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BlockEntityControllerBase.class);
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

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

    @Persisted @Getter
    protected boolean formed = false;
    @Getter
    private StructurePattern pattern;
    @Getter @Persisted
    private ResourceLocation id;
    private int ticks = 0;
    @Persisted
    private boolean needCheckBus = true;

    @Getter
    private final long offset = FantasyStructure.RND.nextLong();
    @Getter
    private final List<IBus> buses = new ArrayList<>();
    @Persisted
    private final RecipeLogic recipeLogic;
    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> recipeCapabilityProxies;

    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings({"UnstableApiUsage"})
    public BlockEntityControllerBase(BlockEntityType<?> entityType,
                                     BlockPos pos, BlockState state,
                                     ResourceLocation patternId) {
        super(entityType, pos, state);
        this.recipeLogic = createRecipeLogic();
        this.id = patternId;
        this.recipeCapabilityProxies = Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            initPattern();
            checkStructure();
        }
    }

    public void serverTick() {
        if(level == null || level.isClientSide) return;

        ticks++;
        if(ticks % 40 == 0) {
            checkStructure();
        }
        serverTickInternal();
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
        if (pattern == null && getLevel() != null && !getLevel().isClientSide) {
            this.pattern = BlueprintManager.getPattern(id, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
    }

    protected void checkStructure() {
        if (pattern == null) initPattern();
        if (pattern == null) return;

        if(needCheckBus) {
            formed = pattern.matches(level, worldPosition, buses);
            initRecipeCapabilityProxies();
            if(formed) {
                this.needCheckBus = false;
                recipeLogic.setStatus(true);
            }
        } else {
            var lastFormed = formed;
            formed = pattern.matches(level, worldPosition);
            if(lastFormed && !formed) {
                markDirty();
            }
        }
    }

    public void autoBuild(ItemStack builderStack, boolean isCreative) {
        if(pattern == null) initPattern();
        if(level != null && !level.isClientSide) {
            StructureBuilderManager.startBuild(level, worldPosition, this.pattern, builderStack, isCreative);
        }
    }

    public void markDirty() {
        needCheckBus = true;
        buses.clear();
    }

    @Override
    public @Nullable FSRecipe getModifyRecipe(FSRecipe recipe) {
        if(getMaxParallel(recipe) == null) return recipe; // This is necessary, as getMaxParallel() will be changed to return null.
        recipe = recipe.copy(getMaxParallel(recipe), false);
        return recipe;
    }

    @Override
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

    private void initRecipeCapabilityProxies() {
        recipeCapabilityProxies.clear();
        for(var bus : buses) {
            if(!recipeCapabilityProxies.contains(bus.getIo(), bus.getRecipeCapability())) {
                recipeCapabilityProxies.put(bus.getIo(), bus.getRecipeCapability(), new ArrayList<>());
            }
            recipeCapabilityProxies.get(bus.getIo(), bus.getRecipeCapability()).add(bus.getRecipeHandler());
        }
    }
}
