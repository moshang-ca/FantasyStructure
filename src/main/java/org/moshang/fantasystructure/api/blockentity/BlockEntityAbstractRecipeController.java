package org.moshang.fantasystructure.api.blockentity;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockEntityAbstractRecipeController extends BlockEntityAbstractController implements IRecipeMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(BlockEntityAbstractRecipeController.class, BlockEntityAbstractController.MANAGED_FIELD_HOLDER);
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
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Getter
    private final long offset = FantasyStructure.RND.nextLong();
    @Persisted
    private final RecipeLogic recipeLogic;
    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> recipeCapabilityProxies;
    private final ContentModifier parallel = new ContentModifier(5, 0);
    private final List<ISubscription> busSubscriptions = new ArrayList<>();

    @SuppressWarnings({"UnstableApiUsage"})
    public BlockEntityAbstractRecipeController(BlockEntityType<?> entityType,
                                               BlockPos pos, BlockState state,
                                               ResourceLocation controllerId) {
        super(entityType, pos, state, controllerId);
        this.recipeLogic = createRecipeLogic();
        this.recipeCapabilityProxies = Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);
    }


    @Override
    public void setRemoved() {
        super.setRemoved();

        busSubscriptions.forEach(ISubscription::unsubscribe);
        busSubscriptions.clear();
    }

    @Override
    protected void serverTickInternal() {
        if(runRecipeLogic()) {
            recipeLogic.serverTick();
        }
    }

    public boolean runRecipeLogic() {
        return IRecipeMachine.super.runRecipeLogic();
    }

//    public void onRotate() {
//        initPattern();
//        setChanged();
//    }


    @Override
    public void onUpgrade() {
        super.onUpgrade();
        if(upgradeInv != null) {
            for(int i = 0; i < upgradeInv.getSlots(); ++i) {
                var stack = upgradeInv.getStackInSlot(i);
                if(!stack.isEmpty()) {
                    if(true) {  // This will be replaced by a type check.
                        parallel.addAddition(1);
                    }
                }
            }
        }
    }

    @Override
    public void onFormed() {
        resetRecipeCapabilityProxies();
    }

    @Override
    public void onDeformed(boolean deformed) {
        recipeCapabilityProxies.clear();

    }

    @RPCMethod
    @Override
    protected void setFormed(boolean formed) {
        super.setFormed(formed);
        recipeLogic.setStatus(formed);
    }

    @Override
    @Nullable
    public ContentModifier getMaxParallel(FSRecipe recipe) {
        return parallel;
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
}
