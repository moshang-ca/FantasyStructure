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

package org.moshang.fantasystructure.api.recipe.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.utils.NBTToJsonConverter;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.crafting.CraftingHelper;

public interface IContentSerializer<T> {
    default void toNetwork(FriendlyByteBuf buf, T content) {
        buf.writeUtf(LDLib.GSON.toJson(toJson(content)));
    }

    default T fromNetwork(FriendlyByteBuf buf) {
        return fromJson(LDLib.GSON.fromJson(buf.readUtf(), JsonElement.class));
    }

    default Tag toNBT(T content) {
        return CraftingHelper.getNBT(toJson(content));
    }

    default T fromNBT(Tag nbt) {
        return fromJson(NBTToJsonConverter.getObject(nbt));
    }

    T fromJson(JsonElement json);
    JsonElement toJson(T content);
    T of(Object o);
    T copyWithModifier(T content, ContentModifier modifier);
    T copyInner(T content);

    default T deepCopyInner(T content) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        toNetwork(buf, content);
        return fromNetwork(buf);
    }

    default Content fromJsonContent(JsonElement json) {
        JsonObject obj = json.getAsJsonObject();
        T inner = fromJson(obj.get("content"));
        boolean perTick = obj.has("perTick") && obj.get("perTick").getAsBoolean();
        float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1f;
        String slotName = obj.has("slotName") ? obj.get("slotName").getAsString() : null;
        return new Content(inner, perTick, chance, slotName);
    }

    @SuppressWarnings("unchecked")
    default JsonElement toJsonContent(Content content) {
        JsonObject obj = new JsonObject();
        obj.add("content", toJson((T) content.getContent()));
        obj.addProperty("perTick", content.isPerTick());
        obj.addProperty("chance", content.getChance());
        if(!content.getSlotName().isEmpty()) {
            obj.addProperty("slotName", content.getSlotName());
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    default void toNetworkContent(FriendlyByteBuf buf, Content content) {
        T inner = (T) content.getContent();
        toNetwork(buf, inner);
        buf.writeBoolean(content.isPerTick());
        buf.writeFloat(content.getChance());
        buf.writeBoolean(!content.getSlotName().isEmpty());
        if(!content.getSlotName().isEmpty()) {
            buf.writeUtf(content.getSlotName());
        }
    }

    default Content fromNetworkContent(FriendlyByteBuf buf) {
        T inner = fromNetwork(buf);
        boolean perTick = buf.readBoolean();
        float chance = buf.readFloat();
        String slotName = null;
        if(buf.readBoolean()) {
            slotName = buf.readUtf();
        }
        return new Content(inner, perTick, chance, slotName);
    }

    default Content fromNBT(CompoundTag tag) {
        T content = fromNBT(tag.get("content"));
        boolean perTick = tag.getBoolean("perTick");
        float chance = tag.getFloat("chance");
        String slotName = tag.getString("slotName");
        return new Content(content, perTick, chance, slotName);
    }

    default CompoundTag toNBT(Content content) {
        CompoundTag tag = new CompoundTag();
        tag.put("content", toNBT((T) content.getContent()));
        tag.putBoolean("perTick", content.isPerTick());
        tag.putFloat("chance", content.getChance());
        tag.putString("slotName", content.getSlotName());
        return tag;
    }
}
