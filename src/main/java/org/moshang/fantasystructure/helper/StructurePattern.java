package org.moshang.fantasystructure.helper;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.data.BlockInfo;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public record StructurePattern(Map<BlockPos, BlockInfo> blockPattern, BlockPos controllerPos) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static StructurePattern createRotated(Map<BlockPos, BlockInfo> pattern, BlockPos controllerPos,
                                                 Direction origin, Direction current) {
        if(origin == current) {
            return new StructurePattern(pattern, controllerPos);
        }

        Rotation rotation = getRelativeRotation(getRelativeAngle(origin, current));
        if(rotation == Rotation.NONE) {
            return new StructurePattern(pattern, controllerPos);
        }

        Map<BlockPos, BlockInfo> rotatedMap = new HashMap<>();

        for(Map.Entry<BlockPos, BlockInfo> entry : pattern.entrySet()) {
            rotatedMap.put(
                    rotatePos(entry.getKey(), rotation),
                    rotateBlockInfo(entry.getValue(), rotation)
            );
        }

        return new StructurePattern(rotatedMap, rotatePos(controllerPos, rotation));
    }

    public boolean matches(Level level, BlockPos pos) {
        for (Map.Entry<BlockPos, BlockInfo> entry : blockPattern.entrySet()) {
            BlockPos worldPos = pos.offset(entry.getKey());
            BlockInfo info = entry.getValue();
            if (!info.matches(level, worldPos)) {
                LOGGER.warn("此处有问题：{}， 原因：expected: {}, now: {}", worldPos, info.getExpectedState(), level.getBlockState(worldPos));

                return false;
            }
        }
        return true;
    }

    @Override
    public Map<BlockPos, BlockInfo> blockPattern() {
        return blockPattern;
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

    private static BlockPos rotatePos(BlockPos from, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-from.getZ(), from.getY(), from.getX());
            case CLOCKWISE_180 -> new BlockPos(-from.getX(), from.getY(), -from.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(from.getZ(), from.getY(), -from.getX());
            default -> from;
        };
    }

    private static BlockInfo rotateBlockInfo(BlockInfo from, Rotation rotation) {
        if(rotation == Rotation.NONE) {
            return from;
        }

        BlockState state = from.getExpectedState();
        BlockState rotatedState = state.rotate(rotation);

        return state == rotatedState ? from : new BlockInfo(rotatedState);
    }
}
