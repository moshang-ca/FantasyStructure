package org.moshang.fantasystructure.helper.builder;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.Config;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.item.ItemAutoBuilder;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class StructureBuilder {
    private final Level level;
    private final BlockPos center;
    private final ItemStack builderStack;
    private final Queue<Map.Entry<BlockPos, BlockInfo>> taskQueue = new LinkedList<>();
    private final Map<BlockPos, BlockInfo> failedBlock = new HashMap<>();

    private boolean building = false;
    private boolean isCreative = false;

    private static final int BLOCKS_PER_TICK = Config.MAX_BLOCK_PLACE_PER_TICK.get();
    private static final Logger LOGGER = LogUtils.getLogger();

    public StructureBuilder(Level level, BlockPos center, StructurePattern pattern, ItemStack builderStack) {
        this.level = level;
        this.center = center;
        this.builderStack = builderStack;

        Map<BlockPos, BlockInfo> patternMap = pattern.blockPattern();

        for(Map.Entry<BlockPos, BlockInfo> entry : patternMap.entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockInfo blockInfo = entry.getValue();
            if(!relativePos.equals(BlockPos.ZERO) && !blockInfo.isAir()) {
                taskQueue.offer(entry);
            }
        }
    }

    public void start(boolean isCreative) {
        if(!building) {
            building = true;
        }
        this.isCreative = isCreative;
    }

    public void tick() {
        if(taskQueue.isEmpty()) return;

        int placed = 0;

        while(!taskQueue.isEmpty() && placed < BLOCKS_PER_TICK) {
            Map.Entry<BlockPos, BlockInfo> entry = taskQueue.poll();
            BlockPos worldPos = center.offset(entry.getKey());
            BlockState targetState = entry.getValue().getExpectedState();

            if(targetState.isAir()) {
                continue;
            }

            BlockState state = level.getBlockState(worldPos);
            if(!state.isAir()) {
                if(!state.equals(targetState)) {
                    failedBlock.put(entry.getKey(), entry.getValue());
                    placed++;
                }
                continue;
            }

            if(!isCreative) {
                ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(targetState.getBlock());
                ItemStack materialStack = ItemAutoBuilder.shrinkMaterials(level, builderStack, blockId);
                if(materialStack != null) {
                    if (!level.setBlock(worldPos, targetState, 2)) {
                        materialStack.shrink(1);
                        failedBlock.put(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                if (!level.setBlock(worldPos, targetState, 2)) {
                    failedBlock.put(entry.getKey(), entry.getValue());
                }
            }
            placed++;
        }

        if(taskQueue.isEmpty()) {
            complete();
        }
    }

    private void complete() {
        building = false;
        if(!failedBlock.isEmpty()) {
            failedBlock.entrySet().forEach(taskQueue::offer);
            failedBlock.clear();
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
