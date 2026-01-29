package org.moshang.fantasystructure.datagen.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.moshang.fantasystructure.FantasyStructure;

import java.util.Set;

public class BlockDropsProvider extends BlockLootSubProvider {
    public BlockDropsProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BuiltInRegistries.BLOCK
                .stream()
                .filter(entry -> entry.getLootTable().getNamespace().equals(FantasyStructure.MODID))
                .toList();
    }

    @Override
    protected void generate() {
        for(var block : getKnownBlocks()) {
            dropSelf(block);
        }
    }
}
