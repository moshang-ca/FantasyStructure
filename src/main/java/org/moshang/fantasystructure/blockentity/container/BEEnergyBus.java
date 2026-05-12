package org.moshang.fantasystructure.blockentity.container;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
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
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.capacity.ComponentEnergyCapacity;
import org.moshang.fantasystructure.api.slot.SyncedEnergyStorage;
import org.moshang.fantasystructure.block.container.BlockEnergyBus;
import org.moshang.fantasystructure.capability.handler.EnergyRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.EnergyRecipeCapability;
import org.moshang.fantasystructure.registry.FSBlockEntities;

public class BEEnergyBus extends BlockEntity implements IBus, IUIHolder.BlockEntityUI {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BEEnergyBus.class);
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

    @Getter @Persisted
    private final SyncedEnergyStorage energyStorage;
    private final LazyOptional<IEnergyStorage> energyHandler;

    @Getter
    private final int capacity;
    @Getter
    private final int maxReceive;
    @Getter
    private final int maxExtract;
    @Getter
    private final IO io;
    @Getter
    private final IRecipeHandler<Integer> recipeHandler;
    @Getter
    private final RecipeCapability<Integer> recipeCapability;

    public BEEnergyBus(BlockPos pos, BlockState state) {
        this(
                FSBlockEntities.ENERGY_BUS_BE.get(),
                pos, state
        );
    }

    public BEEnergyBus(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        ComponentEnergyCapacity type = pBlockState.getValue(BlockEnergyBus.TYPE);
        this.capacity = type.getMaxCapacity();
        this.maxReceive = type.getMaxReceiveCap();
        this.maxExtract = type.getMaxExtractCap();

        this.energyStorage = new SyncedEnergyStorage(capacity, maxReceive, maxExtract);
        this.energyHandler = LazyOptional.of(() -> energyStorage);
        energyStorage.setOnContentsChanged(this::onChanged);

        // For Recipe
        this.io = pBlockState.getValue(BlockEnergyBus.IO_TYPE);
        this.recipeHandler = new EnergyRecipeHandler(io, energyStorage);
        this.recipeCapability = EnergyRecipeCapability.INSTANCE;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    protected WidgetGroup createUI() {
        WidgetGroup root = new WidgetGroup();
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        root.setSize(176, 166);
        var playerInv = new PlayerInventoryWidget();
        playerInv.setSelfPosition(2, 78);
        var energyTexture = new ResourceTexture(FantasyStructure.id("textures/gui/gui_energy_bar.png"));
        var energyWidget = new ProgressWidget(
                () -> (double) energyStorage.getEnergyStored() / energyStorage.getMaxEnergyStored(),
                78, 10, 22, 63,
                new ProgressTexture(
                        IGuiTexture.EMPTY, energyTexture.getSubTexture(.5f, 0, .5f, 1.f)
                )
        ).setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP);

        energyWidget.setDynamicHoverTips(value -> {
            var energyStored = Math.round(capacity * value);
            return LocalizationUtils.format("fantasystructure.gui.energy_bus.storage", energyStored, capacity);
        });
        energyWidget.setBackground(energyTexture.getSubTexture(0, 0, .5f,  1.f));

        root.addWidgets(playerInv, energyWidget);
        return root;
    }

    @Override
    public final ModularUI createUI(Player entityPlayer) {
        return new ModularUI(createUI(), this, entityPlayer);
    }

    public int getEnergyStored() { return energyStorage.getEnergyStored(); }

    public void setEnergyStorageDebug(int value) {
        this.energyStorage.receiveEnergy(value, false);
    }
}
