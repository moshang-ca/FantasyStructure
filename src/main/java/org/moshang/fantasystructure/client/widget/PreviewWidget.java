package org.moshang.fantasystructure.client.widget;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.slot.ExtendedItemStackHandler;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;
import org.moshang.fantasystructure.util.PreviewDummyWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreviewWidget extends WidgetGroup {
    private static PreviewDummyWorld LEVEL;
    private static BlockPos LAST_POS = new BlockPos(0, 50, 0);
    private static final Map<FSStructureDefinitions.StructureDefinition, BlockPos> CACHE = new HashMap<>();
    private static final Map<FSStructureDefinitions.StructureDefinition, StructurePattern> PATTERN_CACHE = new HashMap<>();

    private BlockPos inWorldPos;
    private final StructurePattern pattern;
    private final SceneWidget sceneWidget;
    private final PageScrollableWidget scrollableWidget;
    // private final CycleItemStackHandler materialsItemHandler;
    private final ExtendedItemStackHandler materialsItemHandler;
    private final FSStructureDefinitions.StructureDefinition definition;

    protected PreviewWidget(FSStructureDefinitions.StructureDefinition definition) {
        super(0, 0, 160, 160);
        setClientSideWidget();
        this.definition = definition;

        addWidget(new ImageWidget(3, 4, 154, 154, ResourceBorderTexture.BORDERED_BACKGROUND_INVERSE));
        addWidget(sceneWidget = new SceneWidget(3 + 3, 4 + 3, 128, 128, LEVEL, true)
                .setOnSelected(this::onPosSelected)
                .setRenderFacing(false)
                .setRenderFacing(false));
        if(!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(sceneWidget::useCacheBuffer);
        } else {
            sceneWidget.useCacheBuffer();
        }

        this.pattern = PATTERN_CACHE.computeIfAbsent(definition, newDefinition -> BlueprintManager.getPattern(newDefinition.patternId()));
        this.inWorldPos = CACHE.computeIfAbsent(definition, newDefinition -> initPatternInLevel());

        addWidget(new ImageWidget(3 + 3, 4 + 3, 148, 15,
                new TextTexture(definition.patternId().toLanguageKey(), -1)
                        .setType(TextTexture.TextType.ROLL)
                        .setWidth(148)
                        .setDropShadow(true)));

        this.scrollableWidget = new PageScrollableWidget(3 + 3, 154 - 20, 148, 18)
                .setBackground(ColorPattern.T_BLACK.rectTexture())
                .setOnScroll(this::setupMaterials);
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        this.materialsItemHandler = new ExtendedItemStackHandler(items);
        for(int i = 0; i < items.size(); i++) {
            var slot = new SlotWidget(ItemTransferHelperImpl.toItemTransfer(materialsItemHandler), i, i * 18, 0, false, false)
                    .setBackgroundTexture(ColorPattern.T_BLACK.rectTexture())
                    .setIngredientIO(IngredientIO.INPUT);
            scrollableWidget.addWidget(slot);
        }
        addWidget(scrollableWidget);

        setup();
    }

    private void setup() {
        setupScene();
        setupMaterials(0);
    }

    private void setupScene() {
        List<BlockPos> posToRender;
        if(this.inWorldPos != null && this.pattern != null) {
            posToRender = pattern.blockPattern().keySet().longStream()
                    .mapToObj(relative -> inWorldPos.offset(BlockPos.of(relative)))
                    .toList();
        } else {
            if(this.pattern == null) {
                FantasyStructure.LOGGER.error("Pattern is null for definition: {}", definition.patternId());
                return;
            }
            posToRender = List.of();
        }

        this.sceneWidget.setRenderedCore(posToRender, null);
    }

    private void setupMaterials(int pageChange) {
        var stacks = definition.getMaterials();
        int changed = scrollableWidget.getCurrentPage() + pageChange;
        if(changed > stacks.size() / 8 || changed < 0) return;

        NonNullList<ItemStack> items = NonNullList.create();
        for(int i = 0; i < 8; ++i) {
            var idx = i + changed * 8;
            if(idx < stacks.size()) {
                items.add(stacks.get(idx));
            } else {
                items.add(ItemStack.EMPTY);
            }
        }
        scrollableWidget.setCurrentPage(changed);
        materialsItemHandler.updateStacks(items);
    }

    public static BlockPos locateNextRegion(int range) {
        BlockPos pos = LAST_POS;
        LAST_POS = LAST_POS.offset(range, 0, range);
        return pos;
    }

    private BlockPos initPatternInLevel() {
        if(this.pattern == null) {
            FantasyStructure.LOGGER.error("Cannot initialize pattern in level: pattern is null for definition: {}", definition.patternId());
            return null;
        }
        
        BlockPos emptyRegion = locateNextRegion(500);
        Long2ObjectOpenHashMap<BlockInfo> worldPattern = new Long2ObjectOpenHashMap<>();
        for(var entry : pattern.blockPattern().long2ObjectEntrySet()) {
            if(Thread.currentThread().isInterrupted()) return null;
            BlockPos worldPos = emptyRegion.offset(BlockPos.of(entry.getLongKey()));
            worldPattern.put(worldPos.asLong(), entry.getValue());
        }
        LEVEL.addBlocks(worldPattern);

        return emptyRegion;
    }

    public static PreviewWidget getPreviewWidget(FSStructureDefinitions.StructureDefinition definition) {
        if(LEVEL == null) {
            if(Minecraft.getInstance().level == null) {
                FantasyStructure.LOGGER.error("Try init preview before level initialization");
                throw new IllegalStateException("Try init preview before level initialization");
            }
            LEVEL = new PreviewDummyWorld();
        }
        return new PreviewWidget(definition);
    }

    private void onPosSelected(BlockPos pos, Direction facing) {}

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableBlend();
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
