package org.moshang.fantasystructure.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.moshang.fantasystructure.data.save.StructureWorldSavedData;

@Mod.EventBusSubscriber
public class StructureEventHandler {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide()) return;

        BlockPos pos = event.getPos();
        ServerLevel serverLevel = (ServerLevel) level;
        var savedData = StructureWorldSavedData.getOrCreate(serverLevel);
        savedData.onBlockChanged(pos);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide()) return;

        BlockPos pos = event.getPos();
        ServerLevel serverLevel = (ServerLevel) level;
        var savedData = StructureWorldSavedData.getOrCreate(serverLevel);
        savedData.onBlockChanged(pos);
    }

    @SubscribeEvent
    public static void onBlockNotify(BlockEvent.NeighborNotifyEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) return;

        BlockPos pos = event.getPos();
        ServerLevel serverLevel = (ServerLevel) level;
        var savedData = StructureWorldSavedData.getOrCreate(serverLevel);

        savedData.onBlockChanged(pos);
    }
}
