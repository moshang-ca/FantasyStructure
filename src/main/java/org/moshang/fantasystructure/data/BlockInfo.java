package org.moshang.fantasystructure.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public class BlockInfo {
    @Nullable
    private final BlockState expectedState;
    private final Set<TagKey<Block>> allowedTags;
    @Nullable
    private final CompoundTag tag;

    public BlockInfo(@NotNull BlockState state) {
        this.expectedState = state;
        this.allowedTags = Collections.emptySet();
        this.tag = null;
    }

    public BlockInfo(@NotNull Set<TagKey<Block>> allowedTags) {
        this.expectedState = null;
        this.allowedTags = allowedTags;
        this.tag = null;
    }

    public BlockInfo(BlockState state, CompoundTag tag) {
        this.expectedState = state;
        this.allowedTags = Collections.emptySet();
        this.tag = tag;
    }

    public boolean matches(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if(!allowedTags.isEmpty()) {
            for(TagKey<Block> tag : allowedTags) {
                if(state.is(tag)) {
                    return checkIfNeed(level, pos);
                }
            }
            return false;
        }

        if(expectedState != null) {
            if(!state.equals(expectedState)) {

                return false;
            }
            return checkIfNeed(level, pos);
        }

        return false;
    }

    private boolean checkIfNeed(Level level, BlockPos pos) {
        if(tag == null) {
            return true;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if(be == null) {
            return false;
        }

        CompoundTag nbt = be.serializeNBT();
        return containsAll(nbt, tag);
    }

    private boolean containsAll(CompoundTag actualNBT, CompoundTag requiredNBT) {
        for(String key : requiredNBT.getAllKeys()) {
            if(!actualNBT.contains(key)) {
                return false;
            }
        }
        return true;
    }

    public BlockState getExpectedState() {
        return expectedState;
    }

    public boolean isAir() {
        return expectedState == null || expectedState.isAir();
    }
}
