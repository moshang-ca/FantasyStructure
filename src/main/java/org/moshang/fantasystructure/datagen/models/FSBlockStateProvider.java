package org.moshang.fantasystructure.datagen.models;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.registry.FSBlocks;

public class FSBlockStateProvider extends BlockStateProvider {
    public FSBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, FantasyStructure.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for(var block : FSBlocks.ITEM_INPUT_BUSES) {
            simpleBlockWithItem(block.get(), cubeAll(block.get()));
        }
    }
}
