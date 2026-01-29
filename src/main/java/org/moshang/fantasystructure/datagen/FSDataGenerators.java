package org.moshang.fantasystructure.datagen;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.datagen.loot.FSLootTableProvider;
import org.moshang.fantasystructure.datagen.tags.FSBlockTagProvider;

@Mod.EventBusSubscriber(modid = FantasyStructure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FSDataGenerators {
    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var lookupProvider = event.getLookupProvider();
        var pack = generator.getVanillaPack(true);
        var existingFileHelper = event.getExistingFileHelper();

        pack.addProvider(packOutput -> new FSBlockTagProvider(packOutput, lookupProvider, existingFileHelper));
        pack.addProvider(FSLootTableProvider::new);
    }
}
