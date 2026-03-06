package org.moshang.fantasystructure.helper.blueprint;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.api.block.BlockControllerBase;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.data.blueprint.StateCache;
import org.moshang.fantasystructure.data.blueprint.TagCache;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class Blueprint {
    private final ResourceLocation id;
    private final String name;
    private final int sizeX, sizeY, sizeZ;
    private final BlockPos controllerOffset;
    private final Direction originalDir;
    private final List<String> requiredMods;

    private volatile Map<BlockPos, BlockInfo> patternCache;
    private volatile Long2ObjectOpenHashMap<BlockInfo> patternCached;
    @Getter
    private volatile Map<ResourceLocation, Integer> materialMap;
    private volatile Map<Integer, TagKey<Block>> tagTableMap;
    private volatile BlockState[] blockTypeTable;
    private Path binaryPath;

    private volatile boolean loadingFailed = false;
    private volatile String failureReason;

    private static final Logger LOGGER = LogUtils.getLogger();

    private Blueprint(ResourceLocation id, String name, int sizeX, int sizeY, int sizeZ,
                      BlockPos controllerOffset, List<String> requiredMods, Direction originalDir) {
        this.id = id;
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.controllerOffset = controllerOffset;
        this.originalDir = originalDir;
        this.requiredMods = requiredMods;
    }

    public static Blueprint fromBinary(ResourceLocation id, Path file) throws IOException, BlueprintLoadException {
        try(RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r");
            FileChannel channel = raf.getChannel()) {
            ByteBuffer header = ByteBuffer.allocate(128);
            header.order(ByteOrder.LITTLE_ENDIAN);
            channel.read(header);
            header.flip();

            byte[] magic = new byte[4];
            header.get(magic);
            if(!"FSPB".equals(new String(magic))) {
                throw new BlueprintLoadException("Invalid blueprint format");
            }

            int version = header.getShort();
            int sizeX = header.getShort() & 0xFFFF;
            int sizeY = header.getShort() & 0xFFFF;
            int sizeZ = header.getShort() & 0xFFFF;
            int dataOffset = header.getInt();
            int typeCount = header.getShort() & 0xFFFF;
            byte flags = header.get();
            byte dependencyCount = header.get();
            int controllerX = header.getShort() & 0xFFFF;
            int controllerY = header.getShort() & 0xFFFF;
            int controllerZ = header.getShort() & 0xFFFF;
            BlockPos controllerOffset = new BlockPos(controllerX, controllerY, controllerZ);

            List<String> dependencies = new ArrayList<>();
            for(int i = 0; i < dependencyCount; i++) {
                if(header.remaining() < 1) break;
                int modIdLen = header.get() & 0xFF;
                byte[] modId = new byte[modIdLen];
                header.get(modId);
                dependencies.add(new String(modId));
            }

            List<String> missingMods = new ArrayList<>();
            for(String modId : dependencies) {
                if(!ModList.get().isLoaded(modId)) {
                    missingMods.add(modId);
                    LOGGER.warn("missing mod: {}", modId);
                }
            }
            if(!missingMods.isEmpty()) {
                throw new BlueprintLoadException("Invalid blueprint format");
            }

            channel.position(128);

            BlockState[] stateTable = new BlockState[typeCount];
            Map<Integer, TagKey<Block>> tagTableMap = new HashMap<>();
            loadTypeTable(channel, typeCount, stateTable, tagTableMap);
            Direction dir = Direction.NORTH;
            for(BlockState blockState : stateTable) {
                if(blockState.getBlock() instanceof BlockControllerBase) {
                    dir = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    break;
                }
            }
            Blueprint bp = new Blueprint(
                                id, file.getFileName().toString().replace(".fspb", ""),
                                sizeX, sizeY, sizeZ, controllerOffset, dependencies, dir
                        );
            bp.blockTypeTable = stateTable;
            bp.binaryPath = file;

            return bp;
        } catch (Exception e) {
            if(e instanceof BlueprintLoadException) throw e;
            throw new BlueprintLoadException("Failed to load blueprint:" + e.getMessage(), e);
        }
    }

    private static void loadTypeTable(FileChannel channel, int typeCount, BlockState[] stateTable,
                                              Map<Integer, TagKey<Block>> tagTableMap)
            throws IOException, BlueprintLoadException {
        if(typeCount <= 0 || typeCount > 255)
            throw new BlueprintLoadException("Invalid blueprint typeCount: " + typeCount);

        ByteBuffer typeBuffer = ByteBuffer.allocate(typeCount * 64);
        typeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.read(typeBuffer);
        typeBuffer.flip();

        List<String> missingBlocks = new ArrayList<>();

        for(int i = 0; i < typeCount; i++) {
            if(typeBuffer.remaining() < 1)
                throw new BlueprintLoadException("Type table truncated");

            int stateLen = typeBuffer.get() & 0xFF;
            if(typeBuffer.remaining() < stateLen + 1)
                throw new BlueprintLoadException("Type table entry truncated");

            byte[] stateBytes = new byte[stateLen];
            typeBuffer.get(stateBytes);
            byte props = typeBuffer.get();

            String[] blockTypeStrings = new String(stateBytes).split("\\|", 2);
            BlockState blockState = StateCache.parse(blockTypeStrings[0]);
            if((props & 1) == 1) {
                TagKey<Block> tagKey = TagCache.parse(blockTypeStrings[1]);
                tagTableMap.put(i, tagKey);
            }

            if(!blockTypeStrings[0].equals("Block{minecraft:air}")
                    && blockState == Blocks.AIR.defaultBlockState()){
                missingBlocks.add(blockTypeStrings[0]);
                continue;
            }

            stateTable[i] = blockState;
        }

        if(!missingBlocks.isEmpty()) {
            throw new BlueprintLoadException("Missing blocks: " +
                    String.join(", ", missingBlocks.subList(0, Math.min(5, missingBlocks.size()))) +
                    (missingBlocks.size() > 5 ? "..." : ""));
        }

    }

    public Long2ObjectOpenHashMap<BlockInfo> getPattern() {
        if (loadingFailed) {
            throw new IllegalStateException("Blueprint loading failed: " + failureReason);
        }

        if(patternCache == null) {
            synchronized (this) {
                if(patternCache == null) {
                    try {
                        patternCached = loadPatternInternal();
                    } catch (Exception e) {
                        loadingFailed = true;
                        failureReason = e.getMessage();
                        throw new RuntimeException("Failed to load pattern" + e.getMessage(), e);
                    }
                }
            }
        }

        return patternCached;
    }

    public StructurePattern toStructurePattern(Direction currentDir) {
        return StructurePattern.createRotated(getPattern(), controllerOffset, originalDir, currentDir, sizeZ);
    }

    // Use long2Object map.
    private Long2ObjectOpenHashMap<BlockInfo> loadPatternInternal() {
        if(blockTypeTable == null)
            throw new IllegalStateException("Type table not loaded");

        try(RandomAccessFile raf = new RandomAccessFile(binaryPath.toFile(), "r");
            FileChannel channel = raf.getChannel()) {
            ByteBuffer header = ByteBuffer.allocate(16);
            header.order(ByteOrder.LITTLE_ENDIAN);
            channel.read(header);
            header.position(12);
            int dataOffset = header.getInt();

            channel.position(dataOffset);
            long voxelDataSize = channel.size() - dataOffset;
            ByteBuffer voxelData = ByteBuffer.allocate((int) Math.min(voxelDataSize, Integer.MAX_VALUE));
            channel.read(voxelData);
            voxelData.flip();

            Long2ObjectOpenHashMap<BlockInfo> pattern = new Long2ObjectOpenHashMap<>();
            Map<ResourceLocation, Integer> tempMaterialMap = new HashMap<>();
            decodeRLEToPattern(voxelData, pattern, tempMaterialMap);

            if(!tempMaterialMap.isEmpty()) {
                materialMap = tempMaterialMap;
            }

            return pattern;
        } catch (Exception e) {
            throw new BlueprintLoadException("Blueprint load failed: ", e);
        }
    }

    // Use long2Object map
    private void decodeRLEToPattern(ByteBuffer data, Long2ObjectOpenHashMap<BlockInfo> pattern,
                                    Map<ResourceLocation, Integer> materialMap) {
        int voxelIndex = 0;

        for(int y = 0; y < sizeY && voxelIndex < data.limit(); ++y) {
            for(int z = 0; z < sizeZ && voxelIndex < data.limit(); ++z) {
                for(int x = 0; x < sizeX && voxelIndex < data.limit(); ++x) {
                    byte b = data.get(voxelIndex++);
                    if(b == 0) continue;

                    int typeIdx, count = 1;
                    if(b < 0) {
                        count = -b;
                        if(voxelIndex >= data.limit()) return;
                        typeIdx = data.get(voxelIndex++) & 0xFF;
                    } else {
                        typeIdx = b & 0xFF;
                    }

                    if(typeIdx > 0 && typeIdx <= blockTypeTable.length) {
                        BlockState state = blockTypeTable[typeIdx];
                        if(state != null && !state.is(Blocks.AIR)) {
                            for(int i = 0; i < count; ++i) {
                                int curX = x + i;
                                if(curX >= sizeX) break;

                                BlockPos pos = new BlockPos(curX, y, z).subtract(controllerOffset);
                                pattern.put(pos.asLong(), new BlockInfo(state));
                            }
                        }
                        if(materialMap != null) {
                            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                            if(blockId != null) {
                                materialMap.merge(blockId, count, Integer::sum);
                            }
                        }
                    }

                    if(count > 1) x += (count - 1);
                }
            }
        }
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
