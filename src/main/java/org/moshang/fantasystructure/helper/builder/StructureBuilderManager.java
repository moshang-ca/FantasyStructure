package org.moshang.fantasystructure.helper.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.moshang.fantasystructure.helper.StructurePattern;

import java.util.*;

public class StructureBuilderManager {
    private static final List<StructureBuilder> BUILDERS = new ArrayList<>();
    private static final Map<BlockPos, StructureBuilder> INCOMPLETE = new LinkedHashMap<>(4, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BlockPos, StructureBuilder> eldest) {
            return size() > 4;
        }
    };

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.END) return;

        Iterator<StructureBuilder> iterator = BUILDERS.iterator();
        while(iterator.hasNext()) {
            StructureBuilder builder = iterator.next();
            builder.tick();

            if(!builder.isBuilding()) {
                iterator.remove();
            }
        }
    }

    public static void startBuild(Level level, BlockPos center, StructurePattern pattern) {
        StructureBuilder sbuilder = getOrCreate(level, center, pattern);
        sbuilder.start();
        BUILDERS.add(sbuilder);
    }

    public static StructureBuilder getOrCreate(Level level, BlockPos center, StructurePattern pattern) {
        StructureBuilder sbuilder = INCOMPLETE.remove(center);
        return sbuilder == null ? new StructureBuilder(level, center, pattern) : sbuilder;
    }

    public static void addIncomplete(StructureBuilder builder) {
        if(!INCOMPLETE.containsKey(builder.getCenter())) {
            INCOMPLETE.put(builder.getCenter(), builder);
        }
    }

    public static void removeIncomplete(StructureBuilder builder) {
        INCOMPLETE.remove(builder.getCenter());
    }
}
