package org.moshang.fantasystructure.data;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.api.blockentity.RendererBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Store the block information in the pattern,
 * must have an expected state for default condition.
 */
public class BlockInfo {
    public static final BlockInfo EMPTY = new BlockInfo(Blocks.AIR.defaultBlockState());

    @Getter
    @NotNull private final List<BlockState> allowedStates = new ArrayList<>();
    private BlockEntity lastEntity;

    public BlockInfo(@NotNull BlockState state) {
        this.allowedStates.add(state);
    }

    public BlockInfo(@NotNull List<BlockState> allowedStates) {
        this.allowedStates.addAll(allowedStates);
    }

    public static BlockInfo fromBlockState(@NotNull BlockState state) {
        return new BlockInfo(state);
    }

    public boolean matches(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if(allowedStates.contains(state)) return true;
        return level.getBlockEntity(pos) instanceof RendererBlockEntity;
    }

    public boolean isAir() {
        return allowedStates.isEmpty();
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        var defaultState = allowedStates.get(0);
        if (defaultState.hasBlockEntity() && defaultState.getBlock() instanceof EntityBlock entityBlock) {
            if (lastEntity != null && lastEntity.getBlockPos().equals(pos)) {
                return lastEntity;
            }
            lastEntity = entityBlock.newBlockEntity(pos, defaultState);
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
