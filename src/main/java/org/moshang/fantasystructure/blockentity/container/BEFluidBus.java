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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.BlockEntityAbstractController;
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;
import org.moshang.fantasystructure.api.slot.ExtendedFluidTank;
import org.moshang.fantasystructure.block.container.BlockFluidBus;
import org.moshang.fantasystructure.capability.handler.FluidRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;
import org.moshang.fantasystructure.client.model.MorphingModelData;
import org.moshang.fantasystructure.client.render.MorphingHelper;
import org.moshang.fantasystructure.client.widget.FilterableTankWidget;
import org.moshang.fantasystructure.helper.StructurePattern;
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
    @Getter @Persisted @DescSynced
    private boolean formed = false;
    /** Controller position; {@link BlockPos#ZERO} means not part of a structure yet. */
    @Getter @Persisted @DescSynced
    private BlockPos controllerPos = BlockPos.ZERO;
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

        addSyncUpdateListener("formed", (name, newValue, oldValue) ->
                MorphingHelper.refreshModelData(BEFluidBus.this));
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

    @Override
    public void onStructureFormed(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        formed = true;
        requestModelDataUpdate();
    }

    @Override
    public void onStructureDeformed() {
        this.controllerPos = BlockPos.ZERO;
        formed = false;
        requestModelDataUpdate();
    }

    @Override
    public @NotNull ModelData getModelData() {
        if (level == null || !level.isClientSide) {
            return ModelData.EMPTY;
        }
        Block block = getBlockState().getBlock();
        TextureAtlasSprite overlay = MorphingHelper.resolveOverlaySprite(block);
        TextureAtlasSprite overlayFormed = MorphingHelper.resolveOverlayFormedSprite(block);
        if (!formed || controllerPos == BlockPos.ZERO) {
            return MorphingModelData.build(false, null, null, overlay, overlayFormed);
        }
        if (level.getBlockEntity(controllerPos) instanceof BlockEntityAbstractController controller) {
            StructurePattern pattern = controller.getStructurePattern();
            if (pattern != null) {
                Block overall = MorphingHelper.findDominantNeighbor(level, worldPosition, pattern, controllerPos);
                Block[] perFace = MorphingHelper.findDominantPerFace(level, worldPosition, pattern, controllerPos);
                TextureAtlasSprite overallSprite = overall == null ? null : MorphingHelper.resolveSprite(overall);
                TextureAtlasSprite[] faceSprites = new TextureAtlasSprite[6];
                for (int i = 0; i < 6; i++) {
                    if (perFace[i] != null) {
                        faceSprites[i] = MorphingHelper.resolveSprite(perFace[i]);
                    }
                }
                return MorphingModelData.build(true, overallSprite, faceSprites, overlay, overlayFormed);
            }
        }
        return MorphingModelData.build(true, null, null, overlay, overlayFormed);
    }
}
