package org.moshang.fantasystructure.helper.builder;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class StructureBuilder {
    private final Level level;
    private final BlockPos center;
    private final Queue<Map.Entry<BlockPos, BlockInfo>> taskQueue = new LinkedList<>();
    private final Map<BlockPos, BlockInfo> occupiedBlock = new HashMap<>();

    private boolean building = false;

    private static final int BLOCKS_PER_TICK = 8;
    private static final Logger LOGGER = LogUtils.getLogger();

    public StructureBuilder(Level level, BlockPos center, StructurePattern pattern) {
        this.level = level;
        this.center = center;

        Map<BlockPos, BlockInfo> patternMap = pattern.getBlockPattern();

        for(Map.Entry<BlockPos, BlockInfo> entry : patternMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockInfo blockInfo = entry.getValue();
            if(!relativePos.equals(BlockPos.ZERO) && !blockInfo.isAir()) {
                taskQueue.offer(entry);
            }
        }
    }

    public void start() {
        if(!building) {
            building = true;
        }
    }

    public void tick() {
        if(taskQueue.isEmpty()) return;

        int placed = 0;

        while(!taskQueue.isEmpty() && placed < BLOCKS_PER_TICK) {
            LOGGER.debug("remaining {} blocks", taskQueue.size());
            LOGGER.debug("occupied {} blocks", occupiedBlock.size());
            Map.Entry<BlockPos, BlockInfo> entry = taskQueue.poll();
            BlockPos worldPos = center.offset(entry.getKey());
            BlockState targetState = entry.getValue().getExpectedState();

            if(targetState.isAir()) {
                continue;
            }

            BlockState state = level.getBlockState(worldPos);
            if(!state.isAir()) {
                if(!state.equals(targetState)) {
                    LOGGER.debug("skip one block");
                    occupiedBlock.put(entry.getKey(), entry.getValue());
                    placed++;
                }
                continue;
            }

            if(level.setBlock(worldPos, targetState, 2)) {
                placed++;
            } else {
                taskQueue.offer(entry);
                LOGGER.debug("task add in queue again. block {}, worldPos: {}", entry.getValue().getExpectedState(), worldPos);
            }
        }

        if(taskQueue.isEmpty()) {
            complete();
        }
    }

    private void complete() {
        building = false;
        if(!occupiedBlock.isEmpty()) {
            occupiedBlock.entrySet().forEach(taskQueue::offer);
            occupiedBlock.clear();
            StructureBuilderManager.addIncomplete(this);
        } else {
            StructureBuilderManager.removeIncomplete(this);
        }
    }

    public Level getLevel() {
        return level;
    }
    public BlockPos getCenter() {
        return center;
    }
    public boolean isBuilding() {
        return building;
    }
}
