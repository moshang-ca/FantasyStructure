package org.moshang.fantasystructure.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.api.blockentity.IBus;
import org.moshang.fantasystructure.api.blockentity.IStructureComponent;
import org.moshang.fantasystructure.helper.StructurePattern;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("deprecation")
public class StructureState {
    private static final int MAX_POSITION_SIZE = 256;
    private static final int MAX_CHUNK_CHECKED_PER_TICK = 2;

    private final Long2ObjectOpenHashMap<LongOpenHashSet> structurePosCache = new Long2ObjectOpenHashMap<>();
    private final Queue<Long> chunkCheckQueue = new ConcurrentLinkedQueue<>();
    private final Set<BlockPos> needCheck = new HashSet<>();
    private final LongOpenHashSet unloadedChunks = new LongOpenHashSet();
    private final AtomicBoolean needRecheck = new AtomicBoolean(true); // This marks whether a structure need to be fully checked. In many times this is false.
    @Getter
    private final BlockPos controllerPos;
    @NotNull @Getter
    private StructurePattern pattern; // This can be null when in constructor, but must be set after
    @Getter
    private final List<IStructureComponent> components = new ArrayList<>();
    @Getter
    private final List<IBus> collectedBuses = new ArrayList<>();
    @Getter
    private volatile boolean isLastValid = false;

    public StructureState(BlockPos controllerPos, @NotNull StructurePattern pattern) {
        this.controllerPos = controllerPos;
        setPattern(pattern);
    }

    public void setPattern(StructurePattern pattern) {
        this.pattern = pattern;
        for(var entry : pattern.blockPattern().long2ObjectEntrySet()) {
            BlockPos worldPos = controllerPos.offset(BlockPos.of(entry.getKey()));
            structurePosCache.computeIfAbsent(ChunkPos.asLong(worldPos), v -> new LongOpenHashSet()).add(worldPos.asLong());
        }
    }

    public void markRecheck(BlockPos pos) {
        needCheck.add(pos);
    }

    public void markRecheck() {
        needCheck.clear();
        needRecheck.set(true);
    }

    public void onBlockChanged(BlockPos pos) {
        if(structurePosCache.containsKey(ChunkPos.asLong(pos))) {
            if(needCheck.size() + 1 > MAX_POSITION_SIZE) {
                markRecheck();
                return;
            }
            markRecheck(pos);
        }
    }

    public boolean tickCheck(Level level) {
        if(!needCheck.isEmpty()) {
            return handlePosCheck(level);
        }

        if(needRecheck.get() && chunkCheckQueue.isEmpty()) {
            initQueue();
        } else if(!unloadedChunks.isEmpty()) {
            initUnloadQueue();
        }

        if(!chunkCheckQueue.isEmpty()) {
            return handleChunkCheck(level);
        }

        return isLastValid;
    }

    public void notifyAllComponents(boolean isFormed) {
        if(isFormed) {
            components.forEach(component -> component.onStructureFormed(controllerPos));
        } else {
            components.forEach(IStructureComponent::onStructureDeformed);
        }
    }

    private void initQueue() {
        chunkCheckQueue.clear();
        for(var entry : structurePosCache.keySet()) {
            chunkCheckQueue.offer(entry);
        }
    }

    private void initUnloadQueue() {
        chunkCheckQueue.clear();
        for(var entry : unloadedChunks) {
            chunkCheckQueue.offer(entry);
        }
        unloadedChunks.clear();
    }

    private boolean handlePosCheck(Level level) {
        boolean allValid = true;
        var it = needCheck.iterator();
        while (it.hasNext()) {
            var pos = it.next();
            if(!level.isLoaded(pos)) {
                unloadedChunks.add(ChunkPos.asLong(pos));
                it.remove();
                continue;
            }
            allValid = pattern.matches(level, controllerPos, pos);
            if(allValid) {
                it.remove();
                if(level.getBlockEntity(pos) instanceof IStructureComponent component) {
                    components.add(component);
                    if (component instanceof IBus bus) {
                        collectedBuses.add(bus);
                    }
                }
            } else {
                break;
            }
        }
        this.isLastValid = allValid;
        return isLastValid;
    }

    private boolean handleChunkCheck(Level level) {
        int checked = 0;
        boolean allValid = true;
        components.clear();
        collectedBuses.clear();

        while(checked < MAX_CHUNK_CHECKED_PER_TICK && !chunkCheckQueue.isEmpty()) {
            var chunkPosLong = chunkCheckQueue.poll();
            var positions = structurePosCache.get(chunkPosLong);
            if(positions == null) continue;

            for(var pos : positions) {
                BlockPos pos1 = BlockPos.of(pos);
                if(!level.isLoaded(pos1)) {
                    unloadedChunks.add(chunkPosLong);
                    continue;
                }
                boolean matches = pattern.matches(level, controllerPos, pos1);
                if(!matches) {
                    allValid = false;
                    needCheck.add(pos1);
                    continue;
                }
                if(level.getBlockEntity(pos1) instanceof IStructureComponent component) {
                    components.add(component);
                    if (component instanceof IBus bus) {
                        collectedBuses.add(bus);
                    }
                }
            }
            checked++;
        }

        if(chunkCheckQueue.isEmpty()) {
            if(needRecheck.get()) {
                needRecheck.set(false);
                isLastValid = allValid;
            }
        }
        return isLastValid;
    }

    public Iterable<Long> getAllPositions() {
        return () -> structurePosCache.values().stream()
                .flatMap(LongOpenHashSet::stream)
                .iterator();
    }
}
