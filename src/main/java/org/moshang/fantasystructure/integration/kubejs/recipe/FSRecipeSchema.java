package org.moshang.fantasystructure.integration.kubejs.recipe;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import dev.latvian.mods.kubejs.fluid.FluidLike;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.fluid.InputFluid;
import dev.latvian.mods.kubejs.fluid.OutputFluid;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.ListJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.api.recipe.FSRecipeSerializer;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.api.recipe.content.Content;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;
import org.moshang.fantasystructure.api.recipe.ingredient.SizedIngredient;
import org.moshang.fantasystructure.capability.recipe.EnergyRecipeCapability;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

import java.util.*;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true, chain = true)
@SuppressWarnings("unused")
public class FSRecipeSchema {
    public static final RecipeSchema SCHEMA = new RecipeSchema(FSRecipeJS.class, FSRecipeJS::new);

    protected static class FSRecipeJS extends RecipeJS {
        @FunctionalInterface
        public interface RecipeBuilder extends Consumer<FSRecipeJS> {}
        @Nullable
        private final FSRecipeType recipeType;
        private final Map<RecipeCapability<?>, List<Content>> inputs = new HashMap<>();
        private final Map<RecipeCapability<?>, List<Content>> outputs = new HashMap<>();
        private int duration = 20;
        private int priority;
        @Setter
        private float chance = 1.f;
        private CompoundTag data = new CompoundTag();
        private boolean perTick;

        public FSRecipeJS(@Nullable FSRecipeType recipeType) {
            this.recipeType = recipeType;
        }

        public FSRecipeJS() {
            this(null);
        }

        public FSRecipeJS duration(int duration) {
            this.duration = duration;
            save();
            return this;
        }

        public FSRecipeJS priority(int priority) {
            this.priority = priority;
            save();
            return this;
        }

        public FSRecipeJS addData(String key, Tag data) {
            this.data.put(key, data);
            save();
            return this;
        }

        public FSRecipeJS addData(String key, String data) {
            this.data.putString(key, data);
            save();
            return this;
        }

        public FSRecipeJS addData(String key, boolean data) {
            this.data.putBoolean(key, data);
            save();
            return this;
        }

        public FSRecipeJS addData(String key, double data) {
            this.data.putDouble(key, data);
            save();
            return this;
        }

        public FSRecipeJS perTick(boolean perTick) {
            this.perTick = perTick;
            return this;
        }

        public FSRecipeJS perTick(RecipeBuilder builder) {
            boolean lastState = this.perTick;
            this.perTick = true;
            builder.accept(this);
            this.perTick = lastState;
            return this;
        }

        public FSRecipeJS chance(float chance, RecipeBuilder builder) {
            float lastChance = this.chance;
            this.chance = chance;
            builder.accept(this);
            this.chance = lastChance;
            return this;
        }

        public FSRecipeJS inputs(RecipeCapability<?> capability, Object... objs) {
            inputs.computeIfAbsent(capability, cap -> new ArrayList<>())
                    .addAll(Arrays.stream(objs)
                            .filter(Objects::nonNull)
                            .map(capability::of)
                            .map(obj -> new Content(obj, perTick, chance))
                            .toList());
            save();
            return this;
        }

        public FSRecipeJS outputs(RecipeCapability<?> capability, Object... objs) {
            outputs.computeIfAbsent(capability, cap -> new ArrayList<>())
                    .addAll(Arrays.stream(objs)
                            .filter(Objects::nonNull)
                            .map(capability::of)
                            .map(obj -> new Content(obj, perTick, chance))
                            .toList());
            save();
            return this;
        }

        public FSRecipeJS removeInputs(RecipeCapability<?> capability) {
            inputs.remove(capability);
            save();
            return this;
        }

        public FSRecipeJS removeOutputs(RecipeCapability<?> capability) {
            outputs.remove(capability);
            save();
            return this;
        }

        public FSRecipeJS inputItems(InputItem... items) {
            return inputs(ItemRecipeCapability.INSTANCE, Arrays.stream(items).map(item -> SizedIngredient.create(item.ingredient, item.count)).toArray());
        }

        public FSRecipeJS outputItems(InputItem... items) {
            return outputs(ItemRecipeCapability.INSTANCE, Arrays.stream(items).map(item -> SizedIngredient.create(item.ingredient, item.count)).toArray());
        }

        public FSRecipeJS inputFluids(FluidIngredientJS... fluids) {
            return inputs(FluidRecipeCapability.INSTANCE, Arrays.stream(fluids).map(FluidIngredientJS::ingredient).toArray());
        }

        public FSRecipeJS outputFluids(FluidIngredientJS... fluids) {
            return outputs(FluidRecipeCapability.INSTANCE, Arrays.stream(fluids).map(FluidIngredientJS::ingredient).toArray());
        }

        public FSRecipeJS inputFE(int energy) {
            return inputs(EnergyRecipeCapability.INSTANCE, energy);
        }

        public FSRecipeJS outputFE(int energy) {
            return outputs(EnergyRecipeCapability.INSTANCE, energy);
        }

        private FSRecipeType getRecipeType() {
            if(recipeType == null) {
                var recipeType = FSRecipes.RECIPE_TYPES.get(type.schemaType.id);
                if(recipeType == null) {
                    throw new IllegalStateException("Unknown recipe type: " + type.schemaType.id);
                }
                return recipeType;
            }
            return recipeType;
        }

        @Override
        public void deserialize(boolean merge) {
            super.deserialize(merge);
            var fsRecipe = FSRecipeSerializer.SERIALIZER.fromJson(getOrCreateId(), json);
            inputs.clear();
            outputs.clear();
            inputs.putAll(fsRecipe.inputs());
            outputs.putAll(fsRecipe.outputs());
            data = fsRecipe.data();
            duration = fsRecipe.duration();
            priority = fsRecipe.priority();
        }

        @Override
        public void serialize() {
            json = FSRecipeSerializer.SERIALIZER.toJson(
                    new FSRecipe(getRecipeType(), getOrCreateId(), inputs, outputs, duration, priority, data)
            );
        }

        public FSRecipe buildFSRecipe() {
            return new FSRecipe(getRecipeType(), getOrCreateId(), inputs, outputs, duration, priority, data);
        }
    }

    public record FluidIngredientJS(FluidIngredient ingredient) implements InputFluid, OutputFluid {
        @Override
        public boolean matches(FluidLike other) {
            if(other instanceof FluidStackJS fluidStack) {
                return ingredient.test(FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getNbt()));
            }
            return other.matches(this);
        }

        @Override
        public long kjs$getAmount() {
            return ingredient.getAmount();
        }

        @Override
        public FluidLike kjs$copy(long amount) {
            FluidIngredient newIngredient = ingredient.copy();
            newIngredient.setAmount(amount);
            return new FluidIngredientJS(newIngredient);
        }

        public static FluidIngredientJS of(Object o) {
            if(o instanceof FluidIngredient ingredient) {
                return new FluidIngredientJS(ingredient);
            } else if(o instanceof FluidIngredientJS ingredientJS) {
                return ingredientJS;
            } else if(o instanceof JsonElement json) {
                return new FluidIngredientJS(FluidIngredient.fromJson(json));
            } else if(o instanceof FluidStackJS fluidStack) {
                return new FluidIngredientJS(FluidIngredient.of(FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getNbt())));
            }
            var list = ListJS.of(o);
            if(list != null && !list.isEmpty()) {
                List<FluidStack> stacks = new ArrayList<>();
                for(var obj : list) {
                    var stackJS = FluidStackJS.of(obj);
                    stacks.add(FluidStack.create(stackJS.getFluid(), stackJS.getAmount(), stackJS.getNbt()));
                }
                return new FluidIngredientJS(FluidIngredient.of(stacks.toArray(FluidStack[]::new)));
            } else {
                var stackJS = FluidStackJS.of(o);
                return new FluidIngredientJS(FluidIngredient.of(FluidStack.create(stackJS.getFluid(), stackJS.getAmount(), stackJS.getNbt())));
            }
        }
    }
}
