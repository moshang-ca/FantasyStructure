package org.moshang.fantasystructure.helper.blueprint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.Config;
import org.moshang.fantasystructure.api.blockentity.BlockEntityAbstractController;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class BlueprintEditor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static ExecutorService EXPORTING_THREAD_POOL;

    public static void init() {
        if(EXPORTING_THREAD_POOL != null) return;
        int threadCnt = (int) Math.min(Config.MAX_PROCESSOR.get(), Runtime.getRuntime().availableProcessors() * 1.5);
        EXPORTING_THREAD_POOL = Executors.newFixedThreadPool(Math.max(1, threadCnt));
        LOGGER.info("Initialized Blueprint Editor with {} threads", threadCnt);
    }

    public static void export(Level level, BlockPos pos1, BlockPos pos2,
                                 String registryName, String localizedName,
                                 Path outputFile, BiConsumer<Boolean, String> callback){
        final ExtractionInfo result = extractStructure(level, pos1, pos2);
        if(result.controllerPos == null) {
            callback.accept(false, "Cannot find controller");
            return;
        }

        EXPORTING_THREAD_POOL.submit(() -> {
            try {
                JsonObject root = buildJsonObject(result, registryName, localizedName);
                Files.writeString(outputFile, GSON.toJson(root));
                callback.accept(true, outputFile.toString());
            } catch (IOException e) {
                callback.accept(false, e.getMessage());
            }
        });
    }

    private static ExtractionInfo extractStructure(Level level, BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        int sizeX = maxX - minX;
        int sizeY = maxY - minY;
        int sizeZ = maxZ - minZ;

        BlockPos minCorner = new BlockPos(minX, minY, minZ);

        Map<BlockPos, BlockEntry> positionBlocks = new HashMap<>();
        BlockPos controllerPos = null;
        Direction controllerFacing = Direction.NORTH;

        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    BlockPos worldPos = minCorner.offset(x, y, z);
                    BlockState blockState = level.getBlockState(worldPos);
                    BlockEntity be = level.getBlockEntity(worldPos);

                    if (controllerPos == null && be instanceof BlockEntityAbstractController) {
                        controllerPos = worldPos;
                        controllerFacing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
                    }

                    if (blockState.isAir()) {
                        continue;
                    }

                    String blockId = Objects.requireNonNull(
                            ForgeRegistries.BLOCKS.getKey(blockState.getBlock())).toString();
                    var properties = extractProps(blockState);
                    BlockPos relativePos = new BlockPos(x, y, z);

                    positionBlocks.put(relativePos, new BlockEntry(blockId, properties));
                }
            }
        }

        return new ExtractionInfo(positionBlocks, controllerPos, controllerFacing, minCorner, sizeX, sizeY, sizeZ);
    }

    private static Map<String, String> extractProps(BlockState state) {
        Map<String, String> props = new HashMap<>();
        var defaultState = state.getBlock().defaultBlockState();
        for(var property : state.getProperties()) {
            var value = state.getValue(property);
            if(!defaultState.getValue(property).equals(value)) {
                props.put(property.getName(), value.toString());
            }
        }
        return props;
    }

    private static JsonObject buildJsonObject(ExtractionInfo result, String registryName, String localizedName) {
        JsonObject root = new JsonObject();
        root.addProperty("registry_name", registryName);
        root.addProperty("localized_name", localizedName);

        JsonArray sizes = new JsonArray();
        sizes.add(result.sizeX);
        sizes.add(result.sizeY);
        sizes.add(result.sizeZ);
        root.add("sizes", sizes);

        BlockPos controllerOffset = result.controllerPos.subtract(result.minCorner);
        JsonArray controllerArray = new JsonArray();
        controllerArray.add(controllerOffset.getX());
        controllerArray.add(controllerOffset.getY());
        controllerArray.add(controllerOffset.getZ());
        root.add("controller_offset", controllerArray);

        root.addProperty("original_direction", result.controllerFacing.getName());

        JsonArray partsArray = new JsonArray();
        for(var entry : result.positionBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntry blockEntry = entry.getValue();

            JsonObject part = new JsonObject();
            part.addProperty("x", pos.getX());
            part.addProperty("y", pos.getY());
            part.addProperty("z", pos.getZ());

            JsonArray blocksArray = new JsonArray();
            blocksArray.add(blockEntry.BlockId);
            part.add("blocks", blocksArray);

            JsonObject propsObject = new JsonObject();
            for(var prop : blockEntry.props.entrySet()) {
                propsObject.addProperty(prop.getKey(), prop.getValue());
            }
            part.add("props", propsObject);

            partsArray.add(part);
        }
        root.add("parts", partsArray);

        return root;
    }

    private record ExtractionInfo(Map<BlockPos, BlockEntry> positionBlocks, BlockPos controllerPos,
                                  Direction controllerFacing, BlockPos minCorner, int sizeX, int sizeY, int sizeZ) { }

    private record BlockEntry(String BlockId, Map<String, String> props) { }
}
