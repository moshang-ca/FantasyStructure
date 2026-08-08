/*
 * Copyright (C) 2026 moshang
 *
 * This file is part of FantasyStructure.
 * Contains code adapted from Multiblocked2 (LGPL-3.0).
 *
 * FantasyStructure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */

package org.moshang.fantasystructure.api.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.content.Content;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;
import org.moshang.fantasystructure.api.recipe.ingredient.SizedIngredient;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings({"unused"})
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Accessors(fluent = true, chain = true)
public class FSRecipeBuilder {
    private final Map<RecipeCapability<?>, List<Content>> inputs = new HashMap<>();
    private final Map<RecipeCapability<?>, List<Content>> outputs = new HashMap<>();
    private CompoundTag data = new CompoundTag();

    @Setter
    private ResourceLocation id;
    @Setter
    private FSRecipeType recipeType;
    @Setter
    private int duration = 200;
    @Setter
    private int priority = 0;
    @Setter
    private boolean perTick;
    @Setter
    private String slotName;
    @Setter @NumberRange(range = {0.f, 1.f})
    private float chance = 1.f;
    @Setter @Nullable
    private BiConsumer<FSRecipeBuilder, Consumer<FinishedRecipe>> onSave;

    private FSRecipeBuilder(ResourceLocation id, FSRecipeType recipeType) {
        this.id = id;
        this.recipeType = recipeType;
    }

    private FSRecipeBuilder(FSRecipe copy, FSRecipeType recipeType) {
        this.id = copy.id();
        this.recipeType = recipeType;
        copy.inputs().forEach((k, v) -> this.inputs.put(k, new ArrayList<>(v)));
        copy.outputs().forEach((k, v) -> this.outputs.put(k, new ArrayList<>(v)));
        if (copy.data() != null) {
            this.data = copy.data().copy();
        }
        this.duration = copy.duration();
    }

    public static FSRecipeBuilder of(ResourceLocation id, FSRecipeType recipeType) {
        return new FSRecipeBuilder(id, recipeType);
    }

    public FSRecipeBuilder copy(ResourceLocation id) {
        FSRecipeBuilder copy = new FSRecipeBuilder(id, recipeType);
        this.inputs.forEach((k, v) -> copy.inputs.put(k, new ArrayList<>(v)));
        this.outputs.forEach((k, v) -> copy.outputs.put(k, new ArrayList<>(v)));
        copy.data = this.data.copy();
        copy.duration = this.duration;
        copy.priority = this.priority;
        copy.perTick = this.perTick;
        copy.slotName = this.slotName;
        copy.chance = this.chance;
        copy.onSave = this.onSave;
        return copy;
    }

    public FSRecipeBuilder copyFrom(FSRecipeBuilder builder) {
        return builder.copy(builder.id).onSave(null).recipeType(this.recipeType);
    }

    @SafeVarargs
    public final <T> FSRecipeBuilder input(RecipeCapability<T> capability, T... obj) {
        inputs.computeIfAbsent(capability, k -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, perTick, chance, slotName)).toList());
        return this;
    }

    @SafeVarargs
    public final <T> FSRecipeBuilder output(RecipeCapability<T> capability, T... obj) {
        outputs.computeIfAbsent(capability, k -> new ArrayList<>()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, perTick, chance, slotName)).toList());
        return this;
    }

    public <T> FSRecipeBuilder removeInputs(RecipeCapability<T> capability) {
        inputs.remove(capability);
        return this;
    }

    public <T> FSRecipeBuilder removeOutputs(RecipeCapability<T> capability) {
        outputs.remove(capability);
        return this;
    }

    public FSRecipeBuilder inputItems(ItemStack... inputs) {
        for(ItemStack stack : inputs) {
            if(stack.isEmpty()) {
                throw new IllegalArgumentException(id + " input stack is empty");
            }
        }
        return input(ItemRecipeCapability.INSTANCE, Arrays.stream(inputs).map(SizedIngredient::create).toArray(Ingredient[]::new));
    }

    public FSRecipeBuilder inputItems(Ingredient... inputs) {
        return input(ItemRecipeCapability.INSTANCE, inputs);
    }

    public FSRecipeBuilder inputItems(TagKey<Item> tag, int amount) {
        return inputItems(SizedIngredient.create(tag, amount));
    }

    public FSRecipeBuilder inputItems(TagKey<Item> tag) {
        return inputItems(tag, 1);
    }

    public FSRecipeBuilder inputItems(Item item, int amount) {
        return inputItems(new ItemStack(item, amount));
    }

    public FSRecipeBuilder inputItems(Item item) {
        return inputItems(SizedIngredient.create(new ItemStack(item)));
    }

    // TODO: Add KubeJS integration

    public FSRecipeBuilder outputItems(ItemStack... outputs) {
        for(ItemStack stack : outputs) {
            if(stack.isEmpty()) {
                throw new IllegalArgumentException(id + " output stack is empty");
            }
        }
        return output(ItemRecipeCapability.INSTANCE, Arrays.stream(outputs).map(SizedIngredient::create).toArray(Ingredient[]::new));
    }

    public FSRecipeBuilder outputItems(Item item, int amount) {
        return outputItems(new ItemStack(item, amount));
    }

    public FSRecipeBuilder outputItems(Item item) {
        return outputItems(new ItemStack(item));
    }

    public FSRecipeBuilder notConsume(ItemStack input) {
        float lastChance = chance;
        this.chance = 0;
        inputItems(input);
        this.chance = lastChance;
        return this;
    }

    public FSRecipeBuilder notConsume(Item item) {
        return notConsume(new ItemStack(item));
    }

    @SuppressWarnings("removal")
    public FSRecipeBuilder inputFluids(FluidStack... inputs) {
        return input(FluidRecipeCapability.INSTANCE, Arrays.stream(inputs).map(fluid -> {
            ResourceLocation fluidID = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
            assert fluidID != null;
            return FluidIngredient.of(TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), new ResourceLocation("forge", fluidID.getPath())), fluid.getAmount());
        }).toArray(FluidIngredient[]::new));
    }

    public FSRecipeBuilder inputFluids(FluidIngredient... inputs) {
        return input(FluidRecipeCapability.INSTANCE, inputs);
    }

    public FSRecipeBuilder outputFluids(FluidStack... outputs) {
        return output(FluidRecipeCapability.INSTANCE, Arrays.stream(outputs).map(FluidIngredient::of).toArray(FluidIngredient[]::new));
    }

    public FSRecipeBuilder outputFluids(FluidIngredient... outputs) {
        return output(FluidRecipeCapability.INSTANCE, outputs);
    }

    public FSRecipeBuilder addData(String key, Tag tag) {
        this.data.put(key, tag);
        return this;
    }

    public FSRecipeBuilder addData(String key, int data) {
        this.data.putInt(key, data);
        return this;
    }

    public FSRecipeBuilder addData(String key, boolean data) {
        this.data.putBoolean(key, data);
        return this;
    }

    public FSRecipeBuilder addData(String key, float data) {
        this.data.putFloat(key, data);
        return this;
    }

    public FSRecipeBuilder addData(String key, long data) {
        this.data.putLong(key, data);
        return this;
    }

    public FSRecipeBuilder addData(String key, String data) {
        this.data.putString(key, data);
        return this;
    }

    @SuppressWarnings("removal")
    public FinishedRecipe build() {
        return new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject pJson) {
                FSRecipeSerializer.SERIALIZER.toJson(pJson, buildRawRecipe());
            }

            @Override
            public ResourceLocation getId() {
                return new ResourceLocation(id.getNamespace(), recipeType.getRegistryName().getPath() + "/" + id.getPath());
            }

            @Override
            public RecipeSerializer<?> getType() {
                return FSRecipeSerializer.SERIALIZER;
            }

            @Override
            public @Nullable JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public @Nullable ResourceLocation getAdvancementId() {
                return null;
            }
        };
    }

    public void save(Consumer<FinishedRecipe> p) {
        if(onSave != null) {
            onSave.accept(this, p);
        }
        p.accept(build());
    }

    public FSRecipe saveAsBuiltInRecipe() {
        FSRecipe recipe = buildRawRecipe();
        recipeType.builtInRecipes.put(id, recipe);
        return recipe;
    }

    public FSRecipe buildRawRecipe() {
        return new FSRecipe(recipeType, id, inputs, outputs, duration, priority, data);
    }
}
