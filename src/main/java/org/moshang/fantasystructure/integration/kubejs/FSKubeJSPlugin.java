package org.moshang.fantasystructure.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipesEventJS;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.moshang.fantasystructure.integration.kubejs.event.FSControllerRegistryEventJS;
import org.moshang.fantasystructure.integration.kubejs.event.FSRecipeTypeRegistryEventJS;
import org.moshang.fantasystructure.integration.kubejs.event.FSStartupGroups;
import org.moshang.fantasystructure.integration.kubejs.event.FSStructureDefinitionRegistryEventJS;
import org.moshang.fantasystructure.integration.kubejs.recipe.FSRecipeSchema;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

import java.util.Map;

public class FSKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        super.init();
        RegistryInfo.BLOCK.addType("controller",
                FSControllerRegistryEventJS.ControllerBuilder.class,
                FSControllerRegistryEventJS.ControllerBuilder::new);
    }

    @Override
    public void initStartup() {
        FSStartupGroups.RECIPE_TYPE.post(new FSRecipeTypeRegistryEventJS());
        FSStartupGroups.STRUCTURE.post(new FSStructureDefinitionRegistryEventJS());
    }

    @Override
    public void registerEvents() {
        FSStartupGroups.REGISTRY.register();
    }

    @Override
    public void registerTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        super.registerTypeWrappers(type, typeWrappers);
        typeWrappers.registerSimple(FSRecipeSchema.FluidIngredientJS.class, FSRecipeSchema.FluidIngredientJS::of);
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        super.registerRecipeSchemas(event);
        for (var recipeType : FSRecipes.RECIPE_TYPES) {
            System.out.println("Registering recipe schema for " + recipeType.getRegistryName());
            event.register(recipeType.getRegistryName(), FSRecipeSchema.SCHEMA);
        }
    }

    @Override
    public void injectRuntimeRecipes(RecipesEventJS event, RecipeManager manager, Map<ResourceLocation, Recipe<?>> recipesByName) {
        for (var recipeType : FSRecipes.RECIPE_TYPES) {
            recipeType.onRecipeManagerLoadedKjs(recipesByName);
        }
    }
}
