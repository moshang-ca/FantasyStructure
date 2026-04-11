package org.moshang.fantasystructure.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Map;

public class JsonUtil {
    public static CompoundTag jsonToCompoundTag(JsonObject json) {
        CompoundTag tag = new CompoundTag();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement element = entry.getValue();

            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    tag.putString(key, primitive.getAsString());
                } else if (primitive.isNumber()) {
                    tag.putLong(key, primitive.getAsLong());
                } else if (primitive.isBoolean()) {
                    tag.putBoolean(key, primitive.getAsBoolean());
                }
            } else if (element.isJsonObject()) {
                tag.put(key, jsonToCompoundTag(element.getAsJsonObject()));
            } else if (element.isJsonArray()) {
                tag.put(key, jsonArrayToNbt(element.getAsJsonArray()));
            }
        }
        return tag;
    }

    public static Tag jsonArrayToNbt(JsonArray array) {
        if(array.isEmpty()) return new ListTag();

        JsonElement first = array.get(0);
        if (first.isJsonPrimitive() && first.getAsJsonPrimitive().isNumber()) {
            long[] longArray = new long[array.size()];
            boolean allNumber = true;
            for (int i = 0; i < array.size(); i++) {
                JsonElement elem = array.get(i);
                if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isNumber()) {
                    longArray[i] = elem.getAsLong();
                } else {
                    allNumber = false;
                    break;
                }
            }
            if (allNumber) {
                return new net.minecraft.nbt.LongArrayTag(longArray);
            }
        }

        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (JsonElement elem : array) {
            if (elem.isJsonObject()) {
                list.add(jsonToCompoundTag(elem.getAsJsonObject()));
            } else if (elem.isJsonPrimitive()) {
                JsonPrimitive primitive = elem.getAsJsonPrimitive();
                if (primitive.isString()) {
                    list.add(net.minecraft.nbt.StringTag.valueOf(primitive.getAsString()));
                } else if (primitive.isNumber()) {
                    list.add(net.minecraft.nbt.LongTag.valueOf(primitive.getAsLong()));
                } else if (primitive.isBoolean()) {
                    list.add(net.minecraft.nbt.ByteTag.valueOf((byte) (primitive.getAsBoolean() ? 1 : 0)));
                }
            } else if (elem.isJsonArray()) {
                list.add(jsonArrayToNbt(elem.getAsJsonArray()));
            }
        }
        return list;
    }
}
