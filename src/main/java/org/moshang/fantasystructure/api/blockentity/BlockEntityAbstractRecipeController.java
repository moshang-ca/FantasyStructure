package org.moshang.fantasystructure.api.blockentity;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.MultiRecipeThread;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

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
    private final MultiRecipeThread recipeThread;
    private final Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> recipeCapabilityProxies;

    private final List<ISubscription> busSubscriptions = new ArrayList<>();

    private final int baseParallel;
    @Getter @Persisted
    private int maxParallel;
    private final int parallelLimit;

    @SuppressWarnings({"UnstableApiUsage"})
    public BlockEntityAbstractRecipeController(BlockEntityType<?> entityType,
                                               BlockPos pos, BlockState state,
                                               ResourceLocation controllerId,
                                               int baseParallel, int parallelLimit, int maxThreads) {
        super(entityType, pos, state, controllerId);
        this.recipeCapabilityProxies = Tables.newCustomTable(new EnumMap<>(IO.class), HashMap::new);
        this.baseParallel = baseParallel;
        this.maxParallel = baseParallel;
        this.parallelLimit = parallelLimit;
        this.recipeThread = new MultiRecipeThread(this, maxThreads);
    }

    public BlockEntityAbstractRecipeController(BlockEntityType<?> entityType,
                                               BlockPos pos, BlockState state,
                                               ResourceLocation controllerId) {
        this(entityType, pos, state, controllerId, 1, -1, 1);
    }


    @Override
    public void setRemoved() {
        super.setRemoved();

        busSubscriptions.forEach(ISubscription::unsubscribe);
        busSubscriptions.clear();
    }

    @Override
    protected void serverTickInternal() {
        if(runRecipeThread()) {
            recipeThread.serverTick();
        }
    }

    public boolean runRecipeThread() {
        return IRecipeMachine.super.runRecipeThread();
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
                if(!stack.isEmpty() && stack.getItem().equals(Items.WOODEN_PICKAXE)) {  // This will be replaced by a type check
                    maxParallel++;
                    maxParallel = Math.min(maxParallel, parallelLimit > -1 ? parallelLimit : Integer.MAX_VALUE);
                    upgradeInv.extractItem(i, 1, false);
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
        maxParallel = baseParallel;
        // dropAllParallel();
    }

    @RPCMethod
    @Override
    protected void setFormed(boolean formed) {
        super.setFormed(formed);
        recipeThread.setLogicsStatus(formed);
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
    public MultiRecipeThread getMultiRecipeThread() {
        return recipeThread;
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

            var subscription = bus.addContentChangedListener(recipeThread::wakeUp);
            if(subscription != null) {
                busSubscriptions.add(subscription);
            }
        }
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    protected WidgetGroup createUI() {
        var root = super.createUI();
        var infoWidget = root.getFirstWidgetById("info_widget");

        var threadInfoWidget = new LabelWidget(5, 5, () -> {
            String info = Arrays.stream(recipeThread.getThreads())
                    .map(logic -> LocalizationUtils.format(
                            "fantasystructure.gui.structure_info.thread",
                            logic.getName(), logic.getParallel()
                    )).collect(Collectors.joining("\n"));
            return LocalizationUtils.format(
                    "fantasystructure.gui.structure_info.threads_info",
                    recipeThread.getFreeParallels(), info);
        });
        ((WidgetGroup) infoWidget).addWidget(threadInfoWidget);

        return root;
    }
}
