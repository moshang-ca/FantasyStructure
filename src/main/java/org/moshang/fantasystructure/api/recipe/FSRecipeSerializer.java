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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.utils.NBTToJsonConverter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.content.Content;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FSRecipeSerializer implements RecipeSerializer<FSRecipe> {
    public static final FSRecipeSerializer SERIALIZER = new FSRecipeSerializer();

    public Map<RecipeCapability<?>, List<Content>> capabilitiesFromJson(JsonObject json) {
        Map<RecipeCapability<?>, List<Content>> capabilities = new HashMap<>();
        for(String key : json.keySet()) {
            JsonArray array = json.getAsJsonArray(key);
            RecipeCapability<?> capability = FSRecipes.RECIPE_CAPABILITIES.get(key);
            if(capability != null) {
                List<Content> contents = new ArrayList<>();
                for(JsonElement element : array) {
                    contents.add(capability.getSerializer().fromJsonContent(element));
                }
                capabilities.put(capability, contents);
            }
        }
        return capabilities;
    }

    @Override
    @SuppressWarnings("removal")
    public FSRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
        String recipeType = GsonHelper.getAsString(pSerializedRecipe, "type");
        int duration = pSerializedRecipe.has("duration") ? GsonHelper.getAsInt(pSerializedRecipe, "duration") : 100;
        CompoundTag data = new CompoundTag();
        if(pSerializedRecipe.has("data")) {
            data = CraftingHelper.getNBT(pSerializedRecipe.get("data"));
        }
        Map<RecipeCapability<?>, List<Content>> inputs = capabilitiesFromJson(pSerializedRecipe.has("inputs") ? pSerializedRecipe.getAsJsonObject("inputs") : new JsonObject());
        Map<RecipeCapability<?>, List<Content>> outputs = capabilitiesFromJson(pSerializedRecipe.has("outputs") ? pSerializedRecipe.getAsJsonObject("outputs") : new JsonObject());
        int priority = GsonHelper.getAsInt(pSerializedRecipe, "priority", 0);
        return new FSRecipe((FSRecipeType) ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(recipeType)), pRecipeId, inputs, outputs, duration, priority, data);
    }

    public JsonObject capabilityToJson(Map<RecipeCapability<?>, List<Content>> contents) {
        JsonObject obj = new JsonObject();
        contents.forEach((k, v) -> {
            JsonArray array = new JsonArray();
            for(Content content : v) {
                array.add(k.getSerializer().toJsonContent(content));
            }
            obj.add(FSRecipes.RECIPE_CAPABILITIES.getKey(k), array);
        });
        return obj;
    }

    public JsonObject toJson(JsonObject obj, FSRecipe recipe) {
        obj.addProperty("duration", recipe.duration());
        if(recipe.data() != null && !recipe.data().isEmpty()) {
            obj.add("data", NBTToJsonConverter.getObject(recipe.data()));
        }
        obj.add("inputs", capabilityToJson(recipe.inputs()));
        obj.add("outputs", capabilityToJson(recipe.outputs()));
        if(recipe.priority() != 0) {
            obj.addProperty("priority", recipe.priority());
        }
        return obj;
    }

    public JsonObject toJson(FSRecipe recipe) {
        return toJson(new JsonObject(), recipe);
    }

    public static Tuple<RecipeCapability<?>, List<Content>> entryReader(FriendlyByteBuf buf) {
        RecipeCapability<?> capability = FSRecipes.RECIPE_CAPABILITIES.get(buf.readUtf());
        List<Content> contents = buf.readList(capability.getSerializer()::fromNetworkContent);
        return new Tuple<>(capability, contents);
    }

    public static void entryWriter(FriendlyByteBuf buf, Map.Entry<RecipeCapability<?>, ? extends List<Content>> entry) {
        RecipeCapability<?> capability = entry.getKey();
        List<Content> contents = entry.getValue();
        buf.writeUtf(FSRecipes.RECIPE_CAPABILITIES.getKey(capability));
        buf.writeCollection(contents, capability.getSerializer()::toNetworkContent);
    }

    public static Map<RecipeCapability<?>, List<Content>> tupleToMap(List<Tuple<RecipeCapability<?>, List<Content>>> tuples) {
        Map<RecipeCapability<?>, List<Content>> map = new HashMap<>();
        tuples.forEach(entry -> map.put(entry.getA(), entry.getB()));
        return map;
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable FSRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        String recipeType = pBuffer.readUtf();
        int duration = pBuffer.readInt();
        var inputs = tupleToMap(pBuffer.readCollection(c -> new ArrayList<>(), FSRecipeSerializer::entryReader));
        var outputs = tupleToMap(pBuffer.readCollection(c -> new ArrayList<>(), FSRecipeSerializer::entryReader));
        CompoundTag data = pBuffer.readNbt();
        int priority = pBuffer.readInt();
        return new FSRecipe((FSRecipeType) ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(recipeType)), pRecipeId, inputs, outputs, duration, priority, data);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, FSRecipe pRecipe) {
        pBuffer.writeUtf(pRecipe.recipeType() == null ? "dummy" : pRecipe.recipeType().toString());
        pBuffer.writeVarInt(pRecipe.duration());
        pBuffer.writeCollection(pRecipe.inputs().entrySet(), FSRecipeSerializer::entryWriter);
        pBuffer.writeCollection(pRecipe.outputs().entrySet(), FSRecipeSerializer::entryWriter);
        pBuffer.writeNbt(pRecipe.data());
        pBuffer.writeVarInt(pRecipe.priority());
    }

    public Map<RecipeCapability<?>, List<Content>> capabilityFromNBT(CompoundTag nbt) {
        Map<RecipeCapability<?>, List<Content>> map = new HashMap<>();
        for(String key : nbt.getAllKeys()) {
            List<Content> contents = new ArrayList<>();
            RecipeCapability<?> capability = FSRecipes.RECIPE_CAPABILITIES.get(key);
            if(capability != null) {
                for(var tag : nbt.getList(key, Tag.TAG_COMPOUND)) {
                    contents.add(capability.getSerializer().fromNBT((CompoundTag) tag));
                }
                map.put(capability, contents);
            }
        }
        return map;
    }

    public CompoundTag capabilityToNBT(Map<RecipeCapability<?>, List<Content>> contents) {
        CompoundTag tag = new CompoundTag();
        contents.forEach((k, v) -> {
            ListTag listTag = new ListTag();
            for(var content : v) {
                listTag.add(k.getSerializer().toNBT(content));
            }
            tag.put(k.getName(), listTag);
        });
        return tag;
    }

    @SuppressWarnings("removal")
    public FSRecipe fromNBT(ResourceLocation pRecipeId, CompoundTag nbt) {
        String recipeType = pRecipeId.toString();
        int duration = nbt.getInt("duration");
        var inputs = capabilityFromNBT(nbt.getCompound("inputs"));
        var outputs = capabilityFromNBT(nbt.getCompound("outputs"));
        CompoundTag data = nbt.getCompound("data");
        int priority = nbt.getInt("priority");
        return new FSRecipe((FSRecipeType) ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(recipeType)), pRecipeId, inputs, outputs, duration, priority, data);
    }

    public CompoundTag toNBT(FSRecipe pRecipe) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", pRecipe.recipeType().toString());
        tag.putInt("duration", pRecipe.duration());
        tag.put("inputs", capabilityToNBT(pRecipe.inputs()));
        tag.put("outputs", capabilityToNBT(pRecipe.outputs()));
        tag.put("data", pRecipe.data());
        tag.putInt("priority", pRecipe.priority());
        return tag;
    }
}
