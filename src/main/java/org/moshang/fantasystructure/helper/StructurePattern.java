package org.moshang.fantasystructure.helper;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Map;

public class StructurePattern {
    private final Map<BlockPos, BlockInfo> blockPattern;
    private static final Logger LOGGER = LogUtils.getLogger();

    public StructurePattern(Map<BlockPos, BlockInfo> blockPattern) {
        this.blockPattern = blockPattern;
    }

    public boolean matches(Level level, BlockPos pos) {
        for(Map.Entry<BlockPos, BlockInfo> entry : blockPattern.entrySet()) {
            BlockPos worldPos = pos.offset(entry.getKey());
            BlockInfo info = entry.getValue();
            if(!info.matches(level, worldPos)) {
                LOGGER.warn("此处有问题：{}， 原因：expected: {}, now: {}", worldPos, info.getExpectedState(), level.getBlockState(worldPos));

                return false;
            }
        }
        return true;
    }

    public Map<BlockPos, BlockInfo> getBlockPattern() {
        return blockPattern;
    }
}
