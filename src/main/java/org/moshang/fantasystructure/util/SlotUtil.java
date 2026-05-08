package org.moshang.fantasystructure.util;

import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.api.recipe.content.Content;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;
import org.moshang.fantasystructure.api.recipe.ingredient.SizedIngredient;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SlotUtil {
    private static final Map<FSStructureDefinitions.StructureDefinition, List<ItemStack>> MATERIAL_CACHE = new HashMap<>();

    public static void addJEISlot(IRecipeLayoutBuilder builder, FSRecipe recipe) {
        addSlotForDirection(builder, recipe.inputs(), RecipeIngredientRole.INPUT, 30, 10, 0);
        addSlotForDirection(builder, recipe.outputs(), RecipeIngredientRole.OUTPUT, 120, 10, 100);
    }

    @SuppressWarnings({"rawtypes"})
    private static Map<IIngredientType, List> unwrapContent(Content content) {
        var result = new HashMap<IIngredientType, List>();
        Object o = content.getContent();

        if(o instanceof SizedIngredient ingredient) {
            List<ItemStack> stacks = Arrays.stream(ingredient.getItems()).toList();
            result.put(VanillaTypes.ITEM_STACK, stacks);
        } else if(o instanceof ItemStack itemStack) {
            result.put(VanillaTypes.ITEM_STACK, List.of(itemStack));
        } else if(o instanceof FluidIngredient ingredient) {
            List<FluidStack> fluidStacks = Arrays.stream(ingredient.getStacks()).map(FluidHelperImpl::toFluidStack).toList();
            result.put(ForgeTypes.FLUID_STACK, fluidStacks);
        }

        return result;
    }

    private static void addSlotForDirection(IRecipeLayoutBuilder builder, Map<RecipeCapability<?>, List<Content>> contents,
                                            RecipeIngredientRole role, int x, int y, int index) {
        int slotIndex = 0;
        for(var entry : contents.entrySet()) {
            var contentList = entry.getValue();

            for(Content content : contentList) {
                int slotX = x + (slotIndex % 4) * 18;
                int slotY = y + (slotIndex / 4) * 18;
                addJEISlotForContent(builder, content, role, slotX, slotY, index);
                slotIndex++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addJEISlotForContent(IRecipeLayoutBuilder builder, Content content, RecipeIngredientRole role,
                                             int x, int y, int index) {
        var ingredients = unwrapContent(content);
        if(ingredients.isEmpty()) {
            addInfoSlot(builder, content, x, y);
            return;
        }

        var slotName = "slot_" + index;
        for(var entry : ingredients.entrySet()) {
            var type = entry.getKey();
            var values = entry.getValue();

            for(var value : values) {
                var slotBuilder = builder.addSlot(role, x, y).setSlotName(slotName).addIngredient(type, value);
                if(type == ForgeTypes.FLUID_STACK) {
                    slotBuilder.setFluidRenderer(1, false, 16, 42);
                }
                slotBuilder.addRichTooltipCallback((recipeSlotView, tooltip) -> {
                    if(content.getChance() < 1.f)
                        tooltip.add(Component.translatable("fantasystructure.recipe.tooltip.chance", (int)(content.getChance() * 100)));
                    tooltip.add(Component.translatable("fantasystructure.recipe.tooltip.per_tick", Component.translatable(content.isPerTick() ? "gui.yes" : "gui.no")));
                });
            }
        }
    }

    private static void addInfoSlot(IRecipeLayoutBuilder builder, Content content, int x, int y) {
        var slotBuilder = builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, y);
        slotBuilder.setCustomRenderer(VanillaTypes.ITEM_STACK, new IIngredientRenderer<>() {
            @Override
            public void render(GuiGraphics guiGraphics, ItemStack ingredient) {}

            @Override
            @SuppressWarnings("removal")
            public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
                List<Component> tooltip = new ArrayList<>();
                if(content.getContent() instanceof Number number) {
                    tooltip.add(Component.translatable("fantasystructure.tooltip.amount", number));
                }
                return tooltip;
            }
        });
    }

    public static List<ItemStack> getItems(FSStructureDefinitions.StructureDefinition definition) {
        var materialMap = BlueprintManager.getMaterial(definition.patternId());
        if(materialMap == null || materialMap.isEmpty()) return Collections.emptyList();

        return materialMap.entrySet().stream()
                .map(e -> new ItemStack(e.getKey().get(0), e.getValue()))
                .toList();
    }

}
