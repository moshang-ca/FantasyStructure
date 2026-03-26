package org.moshang.fantasystructure.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.registry.FSBlocks;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;
import org.moshang.fantasystructure.util.SlotUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FSRecipeTypeCategory implements IRecipeCategory<FSRecipe> {
    public static final Function<FSRecipeType, RecipeType<FSRecipe>> TYPES = Util.memoize(recipeMap -> new RecipeType<>(recipeMap.getRegistryName(), FSRecipe.class));

    private final FSRecipeType recipeType;
    private final IDrawable background;
    private final IDrawable icon;

    public FSRecipeTypeCategory(IJeiHelpers helpers, FSRecipeType recipeType) {
        this.recipeType = recipeType;
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 120);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FSBlocks.TEST_CONTROLLER.get()));
    }

    @Override
    public RecipeType<FSRecipe> getRecipeType() {
        return TYPES.apply(recipeType);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(recipeType.getRegistryName().toLanguageKey());
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @SuppressWarnings("DataFlowIssue")
    public static void registerRecipes(IRecipeRegistration registration) {
        for(var recipeType : FSRecipes.RECIPE_TYPES) {
            registration.addRecipes(FSRecipeTypeCategory.TYPES.apply(recipeType),
                    new ArrayList<>(Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType)));
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for(var recipeType : FSRecipes.RECIPE_TYPES) {
            for(var entry : FSStructureDefinitions.DEFINITIONS.entrySet()) {
                var def = entry.getValue();
                if(def.recipeType() == recipeType) {
                    var block = ForgeRegistries.BLOCKS.getValue(entry.getKey());
                    if(block != null) {
                        registration.addRecipeCatalyst(block, TYPES.apply(recipeType));
                    }
                }
            }
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FSRecipe recipe, IFocusGroup focuses) {
        SlotUtil.addJEISlot(builder, recipe);
    }
}
