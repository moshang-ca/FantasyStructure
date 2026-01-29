package org.moshang.fantasystructure.datagen.loot;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class FSLootTableProvider extends LootTableProvider {
    public FSLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(BlockDropsProvider::new, LootContextParamSets.BLOCK)));
    }
}
