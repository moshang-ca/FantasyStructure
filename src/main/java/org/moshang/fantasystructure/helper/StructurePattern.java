package org.moshang.fantasystructure.helper;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.data.BlockInfo;

/**
 * Save structure pattern, using relative position.
 * @param blockPattern pattern from blueprint
 * @param controllerPos
 * @param height
 */
public record StructurePattern(Long2ObjectOpenHashMap<BlockInfo> blockPattern, BlockPos controllerPos, int height) {
    public static StructurePattern createRotated(Long2ObjectOpenHashMap<BlockInfo> pattern, BlockPos controllerPos,
                                                 Direction origin, Direction current, int height) {
        if(origin == current) {
            return new StructurePattern(pattern, controllerPos, height);
        }

        Rotation rotation = getRelativeRotation(getRelativeAngle(origin, current));
        if(rotation == Rotation.NONE) {
            return new StructurePattern(pattern, controllerPos, height);
        }

        Long2ObjectOpenHashMap<BlockInfo> rotatedMap = new Long2ObjectOpenHashMap<>();

        for(var entry : pattern.long2ObjectEntrySet()) {
            rotatedMap.put(
                    rotatePos(BlockPos.of(entry.getLongKey()), rotation),
                    rotateBlockInfo(entry.getValue(), rotation)
            );
        }

        return new StructurePattern(rotatedMap, BlockPos.of(rotatePos(controllerPos, rotation)), height);
    }

    public boolean matches(Level level, BlockPos controllerPos, BlockPos checkPos) {
        var relativePos = checkPos.subtract(controllerPos);
        var info = blockPattern.get(relativePos.asLong());
        if(info == null) {
            // LOGGER.error("info is null, check pos is {}, relative pos is {}, controller pos is {}", checkPos, relativePos, controllerPos);
            return false;
        }
        // LOGGER.warn("Issue at：{}， expected: {}, now: {}", checkPos, info.getExpectedState(), level.getBlockState(checkPos));
        return info.matches(level, checkPos);
    }

    private static int getRelativeAngle(Direction from, Direction to) {
        int fromIdx = from.get2DDataValue();
        int toIdx = to.get2DDataValue();
        return ((toIdx - fromIdx + 4) % 4) * 90;
    }

    private static Rotation getRelativeRotation(int relativeAngle) {
        return switch (relativeAngle) {
            case 90 -> Rotation.CLOCKWISE_90;
            case 180 -> Rotation.CLOCKWISE_180;
            case 270 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static long rotatePos(BlockPos from, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> BlockPos.asLong(-from.getZ(), from.getY(), from.getX());
            case CLOCKWISE_180 -> BlockPos.asLong(-from.getX(), from.getY(), -from.getZ());
            case COUNTERCLOCKWISE_90 -> BlockPos.asLong(from.getZ(), from.getY(), -from.getX());
            default -> from.asLong();
        };
    }

    @SuppressWarnings("deprecation")
    private static BlockInfo rotateBlockInfo(BlockInfo from, Rotation rotation) {
        if(rotation == Rotation.NONE) {
            return from;
        }

        BlockState state = from.getExpectedState();
        BlockState rotatedState = state.rotate(rotation);

        return state == rotatedState ? from : new BlockInfo(rotatedState);
    }
}
