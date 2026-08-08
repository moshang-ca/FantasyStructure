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
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.math.NumberUtils;

public class SerializerInteger implements IContentSerializer<Integer> {
    public static final SerializerInteger INSTANCE = new SerializerInteger();

    private SerializerInteger() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, Integer content) {
        buf.writeInt(content);
    }

    @Override
    public Integer fromNetwork(FriendlyByteBuf buf) {
        return buf.readInt();
    }

    @Override
    public Tag toNBT(Integer content) {
        return IntTag.valueOf(content);
    }

    @Override
    public Integer fromNBT(Tag nbt) {
        if(nbt instanceof IntTag tag) {
            return tag.getAsInt();
        }
        return 0;
    }

    @Override
    public Integer fromJson(JsonElement json) {
        return json.getAsInt();
    }

    @Override
    public JsonElement toJson(Integer content) {
        return new JsonPrimitive(content);
    }

    @Override
    public Integer of(Object o) {
        if(o instanceof Integer integer) {
            return integer;
        } else if(o instanceof Number number) {
            return number.intValue();
        } else if(o instanceof CharSequence charSequence) {
            return NumberUtils.toInt(charSequence.toString(), 1);
        }
        return 0;
    }

    @Override
    public Integer copyWithModifier(Integer content, ContentModifier modifier) {
        return modifier.apply(content).intValue();
    }

    @Override
    public Integer copyInner(Integer content) {
        return content;
    }
}
