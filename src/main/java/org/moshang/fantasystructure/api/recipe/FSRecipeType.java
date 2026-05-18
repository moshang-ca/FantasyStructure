package org.moshang.fantasystructure.api.recipe;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeCapabilityHolder;
import org.moshang.fantasystructure.api.recipe.event.TransferProxyRecipeEvent;
import org.moshang.fantasystructure.util.StringUtil;

import java.util.*;
import java.util.function.Consumer;

@Accessors(chain = true)
public class FSRecipeType implements RecipeType<FSRecipe> {
    public static final FSRecipeType DUMMY = new FSRecipeType(FantasyStructure.id("dummy"));

    @Getter
    private final ResourceLocation registryName;
    @Setter
    @Getter
    private FSRecipeBuilder builder;

    protected final List<RecipeType<?>> proxyRecipeTypes = new ArrayList<>();
    @Getter
    protected final Map<ResourceLocation, FSRecipe> builtInRecipes = new LinkedHashMap<>();

    public FSRecipeType(ResourceLocation registryName, RecipeType<?>... proxyRecipes) {
        this.registryName = registryName;
        builder = FSRecipeBuilder.of(registryName, this);
        proxyRecipeTypes.addAll(Arrays.asList(proxyRecipes));
    }

    // This function will be injected in recipe manager class.
    public void onRecipeManagerLoaded(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> rawRecipes) {
        var recipeTypeMap = rawRecipes.get(this);
        if(recipeTypeMap == null) {
            recipeTypeMap = new HashMap<>();
        } else if(!(recipeTypeMap instanceof HashMap)) {
            recipeTypeMap = new HashMap<>(recipeTypeMap);
        }
        rawRecipes.put(this, recipeTypeMap);
        recipeTypeMap.putAll(builtInRecipes);

        for(var type : proxyRecipeTypes) {
            var recipes = new ArrayList<FSRecipe>();
            for(var recipe : rawRecipes.get(type).entrySet()) {
                FSRecipe fsRecipe = toFSRecipe(type, recipe.getKey(), recipe.getValue());
                if(fsRecipe != null) {
                    recipes.add(fsRecipe);
                    recipeTypeMap.put(fsRecipe.id(), fsRecipe);
                }
            }
        }
    }

    public void onRecipeManagerLoadedKjs(Map<ResourceLocation, Recipe<?>> recipesByName) {
        recipesByName.putAll(builtInRecipes);
    }

    public static FSRecipeType createDefault() {
        return new FSRecipeType(FantasyStructure.id("recipe_type"));
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    public List<FSRecipe> searchRecipes(RecipeManager manager, IRecipeCapabilityHolder holder) {
        if(!holder.hasProxy()) return Collections.emptyList();
        return manager.getAllRecipesFor(this).parallelStream()
                .filter(recipe -> recipe.matchRecipe(holder).isSuccess() && recipe.matchTickRecipe(holder).isSuccess())
                .sorted(Comparator.comparingInt(FSRecipe::priority))
                .toList();
    }

    public FSRecipeType preBuilder(Consumer<FSRecipeBuilder> consumer) {
        consumer.accept(builder);
        return this;
    }

    @SuppressWarnings("removal")
    public FSRecipeBuilder recipeBuilder(ResourceLocation id, Object... append) {
        if(append.length > 0) {
            return builder.copy(new ResourceLocation(id.getNamespace(),
                    id.getPath() + Arrays.stream(append).map(Object::toString).map(StringUtil::formatToLowerCaseUnder).reduce((a, b) -> a + "_" + b)));
        }
        return builder.copy(id);
    }

    public FSRecipeBuilder recipeBuilder(String id, Object... append) {
        return recipeBuilder(FantasyStructure.id(id), append);
    }

    @Nullable
    @SuppressWarnings("removal")
    public FSRecipe toFSRecipe(RecipeType<?> type, ResourceLocation id, Recipe<?> recipe) {
        FSRecipe result = null;
        if(recipe instanceof FSRecipe fsRecipe) {
            var copy = fsRecipe.copy();
            copy.recipeType(this);
            result = copy;
        } else {
            if(!recipe.getIngredients().isEmpty()) {
                var newID = new ResourceLocation(registryName.getNamespace(), registryName.getPath() + "/" + id.getPath());
                var builder = recipeBuilder(newID).recipeType(this);
                for(var ingredient : recipe.getIngredients()) {
                    builder.inputItems(ingredient);
                }
                builder.outputItems(recipe.getResultItem(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
                if(recipe instanceof SmeltingRecipe smeltingRecipe) {
                    builder.duration(smeltingRecipe.getCookingTime());
                }
                result = builder.buildRawRecipe();
            }
        }
        var proxyTypeID = ForgeRegistries.RECIPE_TYPES.getKey(type);
        if(proxyTypeID != null) {
            var event = new TransferProxyRecipeEvent(this, proxyTypeID, type, id, recipe, result);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return null;
            }
            return event.getFsRecipe();
        }
        return result;
    }
}
