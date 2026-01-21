package org.moshang.fantasystructure.helper.blueprint;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.data.blueprint.StateCache;
import org.slf4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;

public class Blueprint {
    private final ResourceLocation id;
    private final String name;
    private final int sizeX, sizeY, sizeZ;
    private final BlockPos controllerOffset;
    private final List<String> requiredMods;

    private volatile Map<BlockPos, BlockInfo> patternCache;
    private volatile BlockState[] blockTypeTable;
    private Path binaryPath;

    private volatile boolean loadingFailed = false;
    private volatile String failureReason;

    private static final Logger LOGGER = LogUtils.getLogger();

    private Blueprint(ResourceLocation id, String name, int sizeX, int sizeY, int sizeZ,
                      BlockPos controllerOffset, List<String> requiredMods) {
        this.id = id;
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.controllerOffset = controllerOffset;
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
            BlockState[] typeTable = loadStateTable(channel, typeCount);

            Blueprint bp = new Blueprint(
                    id, file.getFileName().toString().replace(".fspb", ""),
                    sizeX, sizeY, sizeZ, controllerOffset, dependencies
            );
            bp.blockTypeTable = typeTable;
            bp.binaryPath = file;

            return bp;
        } catch (Exception e) {
            if(e instanceof BlueprintLoadException) throw e;
            throw new BlueprintLoadException("Failed to load blueprint:" + e.getMessage(), e);
        }
    }

    private static BlockState[] loadStateTable(FileChannel channel, int typeCount) throws IOException, BlueprintLoadException {
        if(typeCount <= 0 || typeCount > 255)
            throw new BlueprintLoadException("Invalid blueprint typeCount: " + typeCount);

        ByteBuffer typeBuffer = ByteBuffer.allocate(typeCount * 64);
        typeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.read(typeBuffer);
        typeBuffer.flip();

        BlockState[] stateTable = new BlockState[typeCount];
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

            String blockStateString = new String(stateBytes);
            BlockState blockState = StateCache.parse(blockStateString);

            if(!blockStateString.equals("Block{minecraft:air}")
                    && blockState == Blocks.AIR.defaultBlockState()){
                missingBlocks.add(blockStateString);
                continue;
            }

            stateTable[i] = blockState;
        }

        if(!missingBlocks.isEmpty()) {
            throw new BlueprintLoadException("Missing blocks: " +
                    String.join(", ", missingBlocks.subList(0, Math.min(5, missingBlocks.size()))) +
                    (missingBlocks.size() > 5 ? "..." : ""));
        }

        return stateTable;
    }

    public Map<BlockPos, BlockInfo> getPattern() {
        if (loadingFailed) {
            throw new IllegalStateException("Blueprint loading failed: " + failureReason);
        }

        if(patternCache == null) {
            synchronized (this) {
                if(patternCache == null) {
                    try {
                        patternCache = loadPatternInternal();
                    } catch (Exception e) {
                        loadingFailed = true;
                        failureReason = e.getMessage();
                        throw new RuntimeException("Failed to load pattern" + e.getMessage(), e);
                    }
                }
            }
        }

        return patternCache;
    }

    public StructurePattern toStructurePattern() {
        return new StructurePattern(Collections.unmodifiableMap(getPattern()), controllerOffset);
    }

    private Map<BlockPos, BlockInfo> loadPatternInternal() throws IOException {
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

            Map<BlockPos, BlockInfo> pattern = new HashMap<>();
            decodeRLEToPattern(voxelData, pattern);
            return pattern;
        } catch (Exception e) {
            throw e;
        }
    }

    private void decodeRLEToPattern(ByteBuffer data, Map<BlockPos, BlockInfo> pattern) {
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
                                pattern.put(pos, new BlockInfo(state));
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
