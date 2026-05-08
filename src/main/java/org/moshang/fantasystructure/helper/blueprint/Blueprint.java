package org.moshang.fantasystructure.helper.blueprint;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class Blueprint {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    @Getter
    private final ResourceLocation id;
    private final String name;
    private final int sizeX, sizeY, sizeZ;
    private final BlockPos controllerOffset;
    private final Direction originalDir;
    @Getter
    private final Long2ObjectOpenHashMap<BlockInfo> pattern;
    @Getter
    private final Map<List<Item>, Integer> materialMap;

    public Blueprint(ResourceLocation id, String name,
                        int sizeX, int sizeY, int sizeZ,
                        BlockPos controllerOffset, Direction originalDir,
                        Long2ObjectOpenHashMap<BlockInfo> pattern,
                        Map<List<Item>, Integer> materialMap) {
        this.id = id;
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.controllerOffset = controllerOffset;
        this.originalDir = originalDir;
        this.pattern = pattern;
        this.materialMap = materialMap;
    }

    public static Blueprint fromJson(Path file) throws BlueprintLoadException {
        try {
            String jsonContent = Files.readString(file);
            JsonObject root = GSON.fromJson(jsonContent, JsonObject.class);

            String registryName = root.get("registry_name").getAsString();
            String localizedName = root.has("localized_name") ? root.get("localized_name").getAsString() : registryName;
            ResourceLocation id = FantasyStructure.id(registryName);

            JsonArray sizes = root.getAsJsonArray("sizes");
            int sizeX = sizes.get(0).getAsInt();
            int sizeY = sizes.get(1).getAsInt();
            int sizeZ = sizes.get(2).getAsInt();

            JsonArray controllerArray = root.getAsJsonArray("controller_offset");
            BlockPos controllerOffset = new BlockPos(
                    controllerArray.get(0).getAsInt(),
                    controllerArray.get(1).getAsInt(),
                    controllerArray.get(2).getAsInt()
            );

            Direction dir = Direction.NORTH;
            if(root.has("original_direction")) {
                dir = Direction.valueOf(root.get("original_direction").getAsString().toUpperCase());
            }

            Long2ObjectOpenHashMap<BlockInfo> pattern = new Long2ObjectOpenHashMap<>();
            Map<List<Item>, Integer> materialMap = new HashMap<>();
            JsonArray partArray = root.getAsJsonArray("parts");

            for(var ele : partArray) {
                JsonObject part = ele.getAsJsonObject();

                int x = part.get("x").getAsInt();
                int y = part.get("y").getAsInt();
                int z = part.get("z").getAsInt();
                BlockPos pos = new BlockPos(x, y, z).subtract(controllerOffset);

                List<BlockState> blockStates = new ArrayList<>();
                List<Item> materials = new ArrayList<>();

                var blockArray = part.getAsJsonArray("blocks");
                if(!blockArray.isEmpty()) {
                    for(int i = 0; i < blockArray.size(); ++i) {
                        var state = parseBlockState(blockArray.get(i).getAsString(), part);
                        if(state != null) {
                            blockStates.add(state);
                            materials.add(state.getBlock().asItem());
                        }
                    }
                }

                if(!blockStates.isEmpty()) {
                    BlockInfo info = new BlockInfo(blockStates);
                    pattern.put(pos.asLong(), info);
                    materials.sort(Comparator.comparing(Item::toString));
                    materialMap.merge(materials, 1, Integer::sum);
                }
            }
            LOGGER.info("Loaded blueprint {} with {} blocks", id, pattern.size());
            return new Blueprint(id, localizedName, sizeX, sizeY, sizeZ, controllerOffset, dir, pattern, materialMap);
        } catch (IOException | JsonParseException e) {
            throw new BlueprintLoadException("Failed to load blueprint " + file.getFileName(), e);
        }
    }

    public StructurePattern toStructurePattern(Direction currentDir) {
        return StructurePattern.createRotated(getPattern(), controllerOffset, originalDir, currentDir, sizeZ);
    }

    public StructurePattern toStructurePattern() {
        return toStructurePattern(Direction.NORTH);
    }

    @Nullable
    private static BlockState parseBlockState(String BlockRef, JsonObject part) {
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(BlockRef));
        if(block != null) {
            return parseBlockState(block, part);
        }
        return null;
    }

    private static BlockState parseBlockState(Block block, JsonObject part) {
        BlockState state = block.defaultBlockState();
        JsonObject properties = part.getAsJsonObject("props");
        for(var entry : properties.entrySet()) {
            String propName = entry.getKey();
            String propValue = entry.getValue().getAsString();
            state = applyState(state, propName, propValue);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyState(BlockState state, String propName, String propValue) {
        var property = (Property<T>) state.getBlock().getStateDefinition().getProperty(propName);
        if(property == null) {
            LOGGER.warn("Property [{}] not found in block [{}]", propName, state.getBlock());
            return state;
        }
        return property.getValue(propValue)
                .map(value -> state.setValue(property, value))
                .orElse(state);
    }

    public static class BlueprintLoadException extends RuntimeException {
        public BlueprintLoadException(String message) {
            super(message);
        }

        public BlueprintLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
