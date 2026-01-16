package org.moshang.fantasystructure.helper.blueprint;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.Config;
import org.moshang.fantasystructure.blockentity.BlockEntityController;
import org.slf4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("removal")
public class BlueprintEditor {
    private static ExecutorService EXPORTING_THREAD_POOL;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        if(EXPORTING_THREAD_POOL != null) return;
        int threadCount = (int) (Math.min(Config.MAX_PROCESSOR.get(), Runtime.getRuntime().availableProcessors()) * 1.5);
        EXPORTING_THREAD_POOL = Executors.newFixedThreadPool(Math.max(threadCount, 1));
        LOGGER.info("initialized blueprint editor with {} threads free", threadCount);
    }

    public static void saveBinary(Path outputFile, int sizeX, int sizeY, int sizeZ,
                                  byte[][][] voxels, Map<String, BlockState> blockTypeTable,
                                  Set<String> modDependencies, BlockPos controllerOffset) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(outputFile.toFile());
            FileChannel channel = fos.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(1024*1024);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            buffer.put("FSPB".getBytes());
            buffer.putShort((short) 1);
            buffer.putShort((short) sizeX);
            buffer.putShort((short) sizeY);
            buffer.putShort((short) sizeZ);
            buffer.putInt(0);
            buffer.putShort((short) blockTypeTable.size());
            buffer.put((byte) 0);
            buffer.put((byte) modDependencies.size());
            buffer.putShort((short) controllerOffset.getX());
            buffer.putShort((short) controllerOffset.getY());
            buffer.putShort((short) controllerOffset.getZ());

            for(String modId : modDependencies) {
                byte[] modIdBytes = modId.getBytes();
                buffer.put((byte) modIdBytes.length);
                buffer.put(modIdBytes);
            }

            while(buffer.position() < 128) {
                buffer.put((byte) 0);
            }

            List<String> platte = new ArrayList<>(blockTypeTable.keySet());
            for(String blockId : platte) {
                byte[] idBytes = blockId.getBytes();
                buffer.put((byte) idBytes.length);
                buffer.put(idBytes);
                buffer.put((byte) 0);
            }

            int dataOffset = buffer.position();
            buffer.putInt(12, dataOffset);

            for(int y = 0; y < sizeY; y++) {
                for(int z = 0; z < sizeZ; z++) {
                    int x = 0;
                    while (x < sizeX) {
                        byte current = voxels[y][z][x];
                        int runLength = 1;

                        while (x + runLength < sizeX &&
                                voxels[y][z][x + runLength] == current &&
                                runLength < 127) {
                            runLength++;
                        }

                        if (runLength > 3) {
                            buffer.put((byte) -runLength);
                            buffer.put(current);
                            x += runLength;
                        } else {
                            for (int i = 0; i < runLength; i++) {
                                buffer.put(voxels[y][z][x + i]);
                            }
                            x += runLength;
                        }
                    }
                }
            }
            buffer.flip();
            channel.write(buffer);
        }
    }

    public static boolean exportRegionToBlueprint(Level level, BlockPos pos1, BlockPos pos2,
                                               String name, Path outputFile) throws IOException {
        ExtractionInfo info = extractVoxels(level, pos1, pos2);
        BlockPos minCorner = info.minCorner();
        BlockPos controllerPos = info.controllerPos();
        if(controllerPos == null) {
            return false;
        }

        BlockPos controllerOffset = controllerPos.subtract(minCorner);
        Set<String> modDependencies = extractModDependencies(info.blockTypeTable().keySet());
        saveBinary(
                outputFile,
                info.sizeX(), info.sizeY(), info.sizeZ(),
                info.voxels(), info.blockTypeTable(), modDependencies, controllerOffset
        );
        return true;
    }

    /***
     * This function is used to export a fspb file,
     * converting it into json file
     */
    public static void exportFSPB(String name) {
        if(!name.contains(".fspb")) {
            name = name + ".fspb";
        }
    }

    /**
     * This function is used to export fspb files in batch,
     * converting them into json file
     * @param names All fspb file name you need to export,
     *              needn't have name extension.
     */
    public static void exportFSPB(String[] names) {
        for(String name : names) {
            exportFSPB(name);
        }
    }

    private static Set<String> extractModDependencies(Set<String> blockIds) {
        Set<String> modIds = new HashSet<>();
        for (String blockId : blockIds) {
            if (blockId.contains(":")) {
                String modId = blockId.split(":")[0];
                if (!"minecraft".equals(modId)) {
                    modIds.add(modId);
                }
            }
        }
        return modIds;
    }

    private static ExtractionInfo extractVoxels(Level level, BlockPos pos1, BlockPos pos2) {
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

        Map<String, Byte> blockIdToIndex = new LinkedHashMap<>();
        byte nextIndex = 1;

        byte[][][] voxels = new byte[sizeY][sizeZ][sizeX];

        boolean hasController = false;
        BlockPos controllerPos = null;

        for(int y = 0; y < sizeY; y++) {
            for(int z = 0; z < sizeZ; z++) {
                for(int x = 0; x < sizeX; x++) {
                    BlockPos worldPos = minCorner.offset(x, y, z);
                    BlockState blockState = level.getBlockState(worldPos);
                    BlockEntity be = level.getBlockEntity(worldPos);

                    if(!hasController) {
                        if(be instanceof BlockEntityController controller) {
                            controllerPos = worldPos;
                            hasController = true;
                        }
                    }

                    if(blockState.isAir()) {
                        voxels[y][z][x] = 0;
                        continue;
                    }

                    ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
                    if(blockId == null) {
                        voxels[y][z][x] = 0;
                        continue;
                    }

                    String blockIdStr = blockId.toString();
                    Byte index = blockIdToIndex.get(blockIdStr);
                    if(index == null) {
                        index = nextIndex++;
                        if(index == 0) {
                            index = nextIndex++;
                        }
                        blockIdToIndex.put(blockIdStr, index);
                    }
                    voxels[y][z][x] = index;
                }
            }
        }

        Map<String, BlockState> blockTypeTable = new LinkedHashMap<>();
        for(Map.Entry<String, Byte> entry : blockIdToIndex.entrySet()) {
            String blockId = entry.getKey();
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
            if(block != null) {
                blockTypeTable.put(blockId, block.defaultBlockState());
            }
        }

        return new ExtractionInfo(voxels, blockTypeTable, minCorner, controllerPos, sizeX, sizeY, sizeZ);
    }

    private record ExtractionInfo(byte[][][] voxels, Map<String, BlockState> blockTypeTable, BlockPos minCorner,
                                  BlockPos controllerPos, int sizeX, int sizeY, int sizeZ) {
    }
}
