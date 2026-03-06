package org.moshang.fantasystructure.data;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Store the block information in the pattern,
 * must have an expected state for default condition.
 */
public class BlockInfo {
    public static final BlockInfo EMPTY = new BlockInfo(Blocks.AIR.defaultBlockState());

    @Getter
    @NotNull private final BlockState expectedState;
    @Getter
    private final Set<TagKey<Block>> allowedTags;
    private final byte propertyFlag;
    private BlockEntity lastEntity;

    private static final byte FLAG_HORIZONTAL_FACING = 0x01;
    private static final byte FLAG_FACING = 0x02;
    private static final byte FLAG_AXIS = 0x04;

    public BlockInfo(@NotNull BlockState state) {
        this(state, Collections.emptySet());
    }

    public BlockInfo(@NotNull BlockState state, Set<TagKey<Block>> allowedTags) {
        this.expectedState = state;
        this.allowedTags = allowedTags;
        this.propertyFlag = calculateProperty(state, allowedTags);
    }

    public static BlockInfo fromBlockState(@NotNull BlockState state) {
        return new BlockInfo(state);
    }

    private byte calculateProperty(BlockState state, Set<TagKey<Block>> allowedTags) {
        if(allowedTags.isEmpty()) return 0;

        byte flag = 0;
        if(state.hasProperty(BlockStateProperties.FACING)) {
            flag |= FLAG_FACING;
        } else if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            flag |= FLAG_HORIZONTAL_FACING;
        } else if(state.hasProperty(BlockStateProperties.AXIS)) {
            flag |= FLAG_AXIS;
        }

        return flag;
    }

    public boolean matches(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if(state.equals(expectedState)) return true;

        if(!allowedTags.isEmpty()) {
            for(TagKey<Block> tag : allowedTags) {
                if(state.is(tag)) {
                    if(propertyFlag != 0) {
                        return matchProperties(state);
                    }
                }
            }
            return false;
        }
        return false;
    }

    private boolean matchProperties(BlockState actualState) {
        boolean matched = true;
        if(propertyFlag != 0) {
            if((propertyFlag & FLAG_HORIZONTAL_FACING) != 0) {
                matched = expectedState.getValue(BlockStateProperties.HORIZONTAL_FACING) == actualState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            } else if((propertyFlag & FLAG_FACING) != 0) {
                matched = expectedState.getValue(BlockStateProperties.FACING) == actualState.getValue(BlockStateProperties.FACING);
            } else if((propertyFlag & FLAG_AXIS) != 0) {
                matched = expectedState.getValue(BlockStateProperties.AXIS) == actualState.getValue(BlockStateProperties.AXIS);
            }
        }
        return matched;
    }

    public BlockState createTagBlockState(Block block) {
        if(propertyFlag != 0) {
            if((propertyFlag & FLAG_HORIZONTAL_FACING) != 0) {
                return block.defaultBlockState().setValue(
                        BlockStateProperties.HORIZONTAL_FACING,
                        expectedState.getValue(BlockStateProperties.HORIZONTAL_FACING));
            } else if((propertyFlag & FLAG_FACING) != 0) {
                return block.defaultBlockState().setValue(
                        BlockStateProperties.FACING,
                        expectedState.getValue(BlockStateProperties.FACING));
            } else if((propertyFlag & FLAG_AXIS) != 0) {
                return block.defaultBlockState().setValue(
                        BlockStateProperties.AXIS,
                        expectedState.getValue(BlockStateProperties.AXIS));
            }
        }
        return block.defaultBlockState();
    }

    public boolean isAir() {
        return expectedState.isAir();
    }


    public BlockEntity getBlockEntity(BlockPos pos) {
        if (expectedState.hasBlockEntity() && expectedState.getBlock() instanceof EntityBlock entityBlock) {
            if (lastEntity != null && lastEntity.getBlockPos().equals(pos)) {
                return lastEntity;
            }
            lastEntity = entityBlock.newBlockEntity(pos, expectedState);
            if (lastEntity != null) {
                var compoundTag2 = lastEntity.saveWithoutMetadata();
                var compoundTag3 = compoundTag2.copy();
                if (!compoundTag2.equals(compoundTag3)) {
                    lastEntity.load(compoundTag2);
                }
            }
            return lastEntity;
        }
        return null;
    }

    public BlockEntity getBlockEntity(Level level, BlockPos pos) {
        BlockEntity entity = getBlockEntity(pos);
        if (entity != null) {
            entity.setLevel(level);
        }
        return entity;
    }
}
