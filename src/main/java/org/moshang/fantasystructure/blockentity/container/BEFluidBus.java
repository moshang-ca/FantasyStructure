package org.moshang.fantasystructure.blockentity.container;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.PhantomTankWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidTransferHelperImpl;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;
import org.moshang.fantasystructure.api.slot.ExtendedFluidTank;
import org.moshang.fantasystructure.block.container.BlockFluidBus;
import org.moshang.fantasystructure.capability.handler.FluidRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;
import org.moshang.fantasystructure.client.widget.FilterableTankWidget;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import java.util.ArrayList;
import java.util.List;

public class BEFluidBus extends BlockEntity implements IBus, IUIHolder.BlockEntityUI {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BEFluidBus.class);
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
        setChanged();
    }

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Getter @Persisted @DescSynced
    private final ExtendedFluidTank fluidTank;
    private final LazyOptional<IFluidHandler> handler;

    @Getter
    private final IRecipeHandler<FluidIngredient> recipeHandler;
    @Getter
    private final RecipeCapability<FluidIngredient> recipeCapability;
    @Getter
    private final IO io;
    private final List<Runnable> contentChangedListeners = new ArrayList<>();


    public BEFluidBus(BlockEntityType<?> entityType, BlockPos pos, BlockState state) {
        super(entityType, pos, state);
        var type = state.getValue(BlockFluidBus.TYPE);
        this.fluidTank = createHandler(type.getTanks(), type.getMaxCapacity());
        this.handler = LazyOptional.of(() -> FluidTransferHelperImpl.toFluidHandler(fluidTank));

        // For Recipe
        this.io = state.getValue(BlockFluidBus.IO_TYPE);
        this.recipeHandler = new FluidRecipeHandler(io, fluidTank);
        this.recipeCapability = FluidRecipeCapability.INSTANCE;
    }

    public BEFluidBus(BlockPos pos, BlockState state) {
        this(FSBlockEntities.FLUID_BUS_BE.get(), pos, state);
    }

    private ExtendedFluidTank createHandler(int tanks, long capacity) {
        return ExtendedFluidTank.create(tanks, capacity, this::setChanged);
    }

    private WidgetGroup createUI() {
        WidgetGroup root = new WidgetGroup();
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        root.setSize(176, 202);
        var playerInv = new PlayerInventoryWidget();
        playerInv.setSelfPosition(2, 110);
        for(int i = 0; i < fluidTank.getTanks(); ++i) {
            var filter = new PhantomTankWidget(fluidTank.getFilters()[i], 0, 0);
            var filterWidget = new FilterableTankWidget(fluidTank, i, 13 + i * 22, 43, true, true, filter);
            filterWidget.setSize(18, 61);
            filterWidget.setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP);
            root.addWidgets(filterWidget, filter);
        }

        root.addWidgets(playerInv);
        return root;
    }

    @Override
    public final ModularUI createUI(Player entityPlayer) {
        return new ModularUI(createUI(), this, entityPlayer);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.FLUID_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        contentChangedListeners.forEach(Runnable::run);
    }

    @Override
    public ISubscription addContentChangedListener(Runnable listener) {
        contentChangedListeners.add(listener);
        return () -> contentChangedListeners.remove(listener);
    }
}
