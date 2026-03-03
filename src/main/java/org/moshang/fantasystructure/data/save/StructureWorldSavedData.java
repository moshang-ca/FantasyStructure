package org.moshang.fantasystructure.data.save;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.data.StructureState;

import java.util.*;

public class StructureWorldSavedData extends SavedData {
    private final ServerLevel serverLevel;
    private final Map<BlockPos, StructureState> controllerToState = new HashMap<>();
    private final Long2ObjectOpenHashMap<Set<BlockPos>> posToControllers = new Long2ObjectOpenHashMap<>();

    private StructureWorldSavedData(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
    }

    public static StructureWorldSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> new StructureWorldSavedData(level),
                () -> new StructureWorldSavedData(level),
                "fantasy_structure"
        );
    }

    public void registerStructure(StructureState state) {
        BlockPos controllerPos = state.getControllerPos();
        controllerToState.put(controllerPos, state);
        for(long pos : state.getAllPositions()) {
            posToControllers.computeIfAbsent(pos, k -> Collections.synchronizedSet(new HashSet<>())).add(controllerPos);
        }
        setDirty();
    }

    public void removeStructure(BlockPos controllerPos) {
        StructureState state = controllerToState.remove(controllerPos);
        if(state != null) {
            for(long pos : state.getAllPositions()) {
                var controllers = posToControllers.get(pos);
                if(controllers != null) {
                    controllers.remove(controllerPos);
                    if(controllers.isEmpty()) {
                        posToControllers.remove(pos);
                    }
                }
            }
        }
        setDirty();
    }

    public List<StructureState> getStructureAt(BlockPos pos) {
        posToControllers.forEach((k, v) -> {
        });
        Set<BlockPos> controllers = posToControllers.get(pos.asLong());
        if(controllers == null || controllers.isEmpty()) {
            return Collections.emptyList();
        }

        List<StructureState> result = new ArrayList<>();
        for(BlockPos controllerPos : controllers) {
            StructureState state = controllerToState.get(controllerPos);
            if(state != null) {
                result.add(state);
            }
        }
        return result;
    }

    @Nullable
    public StructureState getStructure(BlockPos controllerPos) {
        return controllerToState.get(controllerPos);
    }

    public Collection<StructureState> getAllStructures() {
        return Collections.unmodifiableCollection(controllerToState.values());
    }

    public Collection<StructureState> getAllControllers() {
        return Collections.unmodifiableCollection(controllerToState.values());
    }

    public void onBlockChanged(BlockPos changedPos) {
        List<StructureState> affected = getStructureAt(changedPos);
        for(var state : affected) {
            state.onBlockChanged(changedPos);
        }
    }

    public boolean isPositionInAnyStructure(BlockPos pos) {
        return posToControllers.containsKey(pos.asLong());
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag pCompoundTag) {
        return pCompoundTag;
    }
}
