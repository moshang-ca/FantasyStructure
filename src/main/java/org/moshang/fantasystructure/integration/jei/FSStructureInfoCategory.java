package org.moshang.fantasystructure.integration.jei;

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.client.widget.PreviewWidget;
import org.moshang.fantasystructure.registry.FSItems;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class FSStructureInfoCategory extends ModularUIRecipeCategory<FSStructureInfoCategory.InfoWrapper> {
    public static class InfoWrapper extends ModularWrapper<PreviewWidget> {
        FSStructureDefinitions.StructureDefinition definition;

        public InfoWrapper(FSStructureDefinitions.StructureDefinition definition) {
            super(PreviewWidget.getPreviewWidget(definition));
            this.definition = definition;
        }

    }

    public static final RecipeType<InfoWrapper> TYPE = new RecipeType<>(FantasyStructure.id("structure_info"), InfoWrapper.class);

    private final IDrawable background;
    private final IDrawable icon;

    public FSStructureInfoCategory(IJeiHelpers helpers) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(160, 160);
        this.icon = helpers.getGuiHelper().createDrawableItemStack(new ItemStack(FSItems.AUTO_BUILDER.get()));
    }

    @Override
    public RecipeType<InfoWrapper> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("fantasystructure.jei.structure_info");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable IDrawable getBackground() {
        return background;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, FSStructureDefinitions.DEFINITIONS.values().stream()
                .map(InfoWrapper::new)
                .toList());
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        for(var blockId : FSStructureDefinitions.DEFINITIONS.keys()) {
            var block = ForgeRegistries.BLOCKS.getValue(blockId);
            if(block != null) {
                registration.addRecipeCatalyst(block.asItem(), TYPE);
            }
        }
    }
}
