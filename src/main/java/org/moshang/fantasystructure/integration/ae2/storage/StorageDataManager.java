package org.moshang.fantasystructure.integration.ae2.storage;

import appeng.api.stacks.AEKey;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.moshang.fantasystructure.FantasyStructure;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class StorageDataManager {
    private static final String DATA_FOLDER = "multiblock_storage";
    private static final String FILE_EXTENSION = ".dat";
    private static final String BACKUP_EXTENSION = ".bak";
    private static final Map<UUID, StorageData> loadedStorages = new HashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static Path storageRoot;

    public static void init(ServerLevel level) {
        Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
        storageRoot = worldRoot.resolve(DATA_FOLDER);
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage data directory", e);
        }
    }


    public static StorageData getOrLoad(UUID uuid) {
        return loadedStorages.computeIfAbsent(uuid, id -> {
            Path path = getStoragePath(uuid);
            return loadFromFile(path, uuid);
        });
    }

    public static void save(UUID uuid) {
        StorageData data = loadedStorages.get(uuid);
        if (data != null && data.isDirty()) {
            Path path = getStoragePath(uuid);
            saveToFile(path, data);
            data.setDirty(false);
        }
    }

    public static void saveAll() {
        for(var entry : loadedStorages.entrySet()) {
            var data = entry.getValue();
            if(data.isDirty()) {
                save(data.getId());
            }
        }
    }

    public static void unload(UUID uuid) {
        var data = loadedStorages.get(uuid);
        if(data != null && data.isDirty()) {
            save(uuid);
        }
    }

    public static void clear() {
        saveAll();
        loadedStorages.clear();
        storageRoot = null;
    }

    private static Path getStoragePath(UUID uuid) {
        if(storageRoot == null) {
            throw new IllegalStateException("StorageDataManager has not been initialized yet");
        }
        return storageRoot.resolve(uuid.toString() + FILE_EXTENSION);
    }

    private static Path getBackupPath(Path path) {
        return path.resolveSibling(path.getFileName().toString() + BACKUP_EXTENSION);
    }

    private static StorageData loadFromFile(Path path, UUID uuid) {
        if(!Files.exists(path)) {
            return new StorageData(uuid);
        }

        try(InputStream fis = Files.newInputStream(path);
            InputStream gzip = new GZIPInputStream(fis);
            InputStreamReader reader = new InputStreamReader(gzip);
            BufferedReader bufferedReader = new BufferedReader(reader)) {

            JsonObject json = GSON.fromJson(bufferedReader, JsonObject.class);
            return deserialize(json, uuid);
        } catch (IOException e) {
            Path backupPath = getBackupPath(path);
            if(Files.exists(backupPath)) {
                try(InputStream fis = Files.newInputStream(path);
                    InputStream gzip = new GZIPInputStream(fis);
                    InputStreamReader reader = new InputStreamReader(gzip);
                    BufferedReader bufferedReader = new BufferedReader(reader)) {

                    JsonObject json = GSON.fromJson(bufferedReader, JsonObject.class);
                    return deserialize(json, uuid);
                } catch (IOException ex) {
                    FantasyStructure.LOGGER.error("Failed to load storage data: {}", ex.getMessage());
                    return new StorageData(uuid);
                }
            }
            return new StorageData(uuid);
        }
    }

    private static void saveToFile(Path path, StorageData storageData) {
        Path tempPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
        Path backupPath = getBackupPath(path);

        try {
            try(OutputStream fos = Files.newOutputStream(tempPath);
                OutputStream gzip = new GZIPOutputStream(fos);
                OutputStreamWriter writer = new OutputStreamWriter(gzip);
                PrintWriter printWriter = new PrintWriter(writer)) {

                JsonObject json = serialize(storageData);
                GSON.toJson(json, printWriter);
            }

            if(Files.exists(path)) {
                Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if(Files.exists(backupPath)) {
                try {
                    Files.move(backupPath, path, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    FantasyStructure.LOGGER.error("Failed to save storage data: {}", ex.getMessage());
                }
            }
            FantasyStructure.LOGGER.error("Failed to save storage data for {} : {} ", storageData.getId(), e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
        }

    }

    private static JsonObject serialize(StorageData data) {
        JsonObject root = new JsonObject();
        root.addProperty("uuid", data.getId().toString());
        root.addProperty("max_types", data.getMaxTypes());
        root.addProperty("max_bytes", data.getMaxBytes());

        JsonObject channelsJson = new JsonObject();
        for(var entry : data.getChannels().entrySet()) {
            String typeName = entry.getKey().getId().toString();
            var channel = entry.getValue();

            JsonObject channelJson = new JsonObject();
            channelJson.addProperty("types", channel.getTypes());
            channelJson.addProperty("count", channel.getCount());
            channelJson.addProperty("used_bytes", channel.getUsedBytes());

            JsonArray items = new JsonArray();
            for(var itemEntry : channel.getAmounts().object2LongEntrySet()) {
                AEKey key = itemEntry.getKey();
                long amount = itemEntry.getLongValue();

                CompoundTag keyTag = key.toTagGeneric();
                JsonObject itemJson = new JsonObject();
                itemJson.addProperty("amount", amount);
                itemJson.add("key", GSON.fromJson(keyTag.toString(), JsonObject.class));
                items.add(itemJson);
            }
            channelJson.add("items", items);
            channelsJson.add(typeName, channelJson);
        }
        root.add("channels", channelsJson);

        return root;
    }

    private static StorageData deserialize(JsonObject json, UUID id) {
        StorageData data = new StorageData(id);

        if(json.has("max_types")) {
            data.setMaxTypes(json.get("max_types").getAsLong());
        }
        if(json.has("max_bytes")) {
            data.setMaxBytes(json.get("max_bytes").getAsLong());
        }

        JsonObject channelsJson = json.getAsJsonObject("channels");
        if(channelsJson != null) {
            for(String typeName : channelsJson.keySet()) {
                JsonObject channelJson = channelsJson.getAsJsonObject(typeName);

                JsonArray items = channelJson.getAsJsonArray("items");
                for(int i = 0; i < items.size(); i++) {
                    JsonObject itemJson = items.get(i).getAsJsonObject();
                    long amount = itemJson.get("amount").getAsLong();

                    JsonObject keyJson = itemJson.getAsJsonObject("key");
                    CompoundTag keyTag = jsonToCompoundTag(keyJson);
                    AEKey key = AEKey.fromTagGeneric(keyTag);
                    if(key != null) {
                        data.restoreItem(key, amount);
                    }
                }
            }
        }
        data.setDirty(false);
        return data;
    }

    private static CompoundTag jsonToCompoundTag(JsonObject json) {
        CompoundTag tag = new CompoundTag();

        for(String key : json.keySet()) {
            JsonElement element = json.get(key);
            if(element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if(primitive.isString()) {
                    tag.putString(key, primitive.getAsString());
                } else if(primitive.isNumber()) {
                    tag.putLong(key, primitive.getAsLong());
                } else if(primitive.isBoolean()) {
                    tag.putBoolean(key, primitive.getAsBoolean());
                }
            } else if(element.isJsonObject()) {
                tag.put(key, jsonToCompoundTag(element.getAsJsonObject()));
            } else if(element.isJsonArray()) {
                tag.putString(key, element.toString());
            }
        }
        return tag;
    }
}
