package org.moshang.fantasystructure.helper.builder;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.Config;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.data.save.StructureWorldSavedData;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.item.ItemAutoBuilder;
import org.slf4j.Logger;

import java.util.*;

public class StructureBuilder {
    @Getter
    private final Level level;
    @Getter
    private final BlockPos center;
    private final ItemStack builderStack;
    private final Queue<Long2ObjectMap.Entry<BlockInfo>> taskQueue = new LinkedList<>();
    private final Long2ObjectOpenHashMap<BlockInfo> failedBlock = new Long2ObjectOpenHashMap<>();

    @Getter
    private boolean building = false;
    private boolean isCreative = false;

    private static final int BLOCKS_PER_TICK = Config.MAX_BLOCK_PLACE_PER_TICK.get();
    private static final Logger LOGGER = LogUtils.getLogger();

    public StructureBuilder(Level level, BlockPos center, StructurePattern pattern, ItemStack builderStack) {
        this.level = level;
        this.center = center;
        this.builderStack = builderStack;

        Long2ObjectOpenHashMap<BlockInfo> patternMap = pattern.blockPattern();

        for(var entry : patternMap.long2ObjectEntrySet()) {
            BlockPos relativePos = BlockPos.of(entry.getLongKey());
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
            var entry = taskQueue.poll();
            BlockInfo blockInfo = entry.getValue();
            BlockPos worldPos = center.offset(BlockPos.of(entry.getLongKey()));
            BlockState targetState = blockInfo.getExpectedState();
            Set<TagKey<Block>> blockTagKeys = blockInfo.getAllowedTags();

            if(targetState.isAir()) {
                continue;
            }

            BlockState state = level.getBlockState(worldPos);
            if(!state.isAir()) {
                if(!state.equals(targetState)) {
                    for(var tagKey : blockTagKeys) {
                        if(!state.is(tagKey)) {
                            failedBlock.put(entry.getLongKey(), entry.getValue());
                            placed++;
                        }
                    }
                }
                continue;
            }

            if(!isCreative) {
                var fluidState = targetState.getFluidState();
                if(fluidState.isEmpty()) {
                    ItemStack materialStack = ItemAutoBuilder.shrinkMaterials(level, builderStack, blockTagKeys, targetState);
                    if (materialStack != null && materialStack.getItem() instanceof BlockItem blockItem) {
                        BlockState materialState = blockInfo.createTagBlockState(blockItem.getBlock());
                        if (!level.setBlock(worldPos, materialState, 2)) {
                            failedBlock.put(entry.getLongKey(), entry.getValue());
                        }
                    }
                } else {
                    Fluid fluid = ItemAutoBuilder.shrinkMaterials(level, builderStack, fluidState);
                    if(!fluid.isSame(Fluids.EMPTY)) {
                        if(!level.setBlock(worldPos, fluid.defaultFluidState().createLegacyBlock(), 2)) {
                            failedBlock.put(entry.getLongKey(), entry.getValue());
                        }
                    }
                }
            } else {
                if (!level.setBlock(worldPos, targetState, 2)) {
                    failedBlock.put(entry.getLongKey(), entry.getValue());
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
            failedBlock.long2ObjectEntrySet().forEach(taskQueue::offer);
            failedBlock.clear();
            StructureBuilderManager.addIncomplete(this);
        } else {
            StructureBuilderManager.removeIncomplete(this);
        }
    }

}
