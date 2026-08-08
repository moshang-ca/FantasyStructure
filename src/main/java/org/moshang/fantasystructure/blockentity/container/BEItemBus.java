package org.moshang.fantasystructure.blockentity.container;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.blockentity.BlockEntityAbstractController;
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.slot.ExtendedItemStackHandler;
import org.moshang.fantasystructure.block.container.BlockItemBus;
import org.moshang.fantasystructure.capability.handler.ItemSlotRecipeHandler;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;
import org.moshang.fantasystructure.client.model.MorphingModelData;
import org.moshang.fantasystructure.client.render.MorphingHelper;
import org.moshang.fantasystructure.client.widget.FixedScrollableWidget;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.registry.FSBlockEntities;

import java.util.ArrayList;
import java.util.List;

public class BEItemBus extends BlockEntity implements IBus, IUIHolder.BlockEntityUI {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BEItemBus.class);
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Override
    public void onChanged() {
        setChanged();
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Getter @Persisted
    private final ExtendedItemStackHandler itemHandler;
    @Getter @Persisted @DescSynced
    private boolean formed = false;
    /** Controller position; {@link BlockPos#ZERO} means not part of a structure yet. */
    @Getter @Persisted @DescSynced
    private BlockPos controllerPos = BlockPos.ZERO;
    private final LazyOptional<IItemHandler> handler;
    @Getter
    private final IRecipeHandler<Ingredient> recipeHandler;
    @Getter
    private final IO io;
    @Getter
    private final RecipeCapability<Ingredient> recipeCapability;
    private final List<Runnable> contentChangedListeners = new ArrayList<>();

    public BEItemBus(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        ComponentItemCapacity type = pBlockState.getValue(BlockItemBus.TYPE);
        this.itemHandler = createHandler(type.getSlots());
        this.itemHandler.setOnContentsChanged(() -> contentChangedListeners.forEach(Runnable::run));
        this.handler = LazyOptional.of(() -> itemHandler);

        // For Recipe
        this.io = pBlockState.getValue(BlockItemBus.IO_TYPE);
        this.recipeHandler = new ItemSlotRecipeHandler(io, itemHandler);
        this.recipeCapability = ItemRecipeCapability.INSTANCE;

        addSyncUpdateListener("formed", (name, newValue, oldValue) ->
                MorphingHelper.refreshModelData(BEItemBus.this));
    }

    public BEItemBus(BlockPos pPos, BlockState pBlockState) {
        this(
                FSBlockEntities.ITEM_BUS_BE.get(),
                pPos, pBlockState
        );
    }

    private ExtendedItemStackHandler createHandler(int size) {
        return new ExtendedItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    protected WidgetGroup createUI() {
        WidgetGroup root = new WidgetGroup();
        root.setSize(176, 222);
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        var playerInv = new PlayerInventoryWidget();
        playerInv.setSelfPosition(2, 124);
        var containerWidget = new FixedScrollableWidget(6, 5, 162, 108)
                .setYBarHeight(17)
                .setYBarStyle(ResourceBorderTexture.BORDERED_BACKGROUND_INVERSE,
                        new ResourceTexture(FantasyStructure.id("textures/gui/scroll_bar.png"), 0, 0, .5f, 1.f))
                .setYScrollBarWidth(14)
                .setUseScissor(true);
        containerWidget.setScrollXOffset(6);
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            containerWidget.addWidget(new SlotWidget(itemHandler.toIItemTransfer(), i, 8 + (i % 8) * 18, 8 + (i / 8) * 18));
        }

        root.addWidgets(playerInv, containerWidget);
        return root;
    }

    @Override
    public final ModularUI createUI(Player entityPlayer) {
        return new ModularUI(createUI(), this, entityPlayer);
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

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }
}
