package org.moshang.fantasystructure.datagen.models;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.block.container.BlockItemBus;
import org.moshang.fantasystructure.registry.FSBlocks;

public class FSBlockStateProvider extends BlockStateProvider {
    private static final int DEFAULT_ANGLE_OFFSET = 180;

    public FSBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, FantasyStructure.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for(var block : FSBlocks.ITEM_INPUT_BUSES) {
            ModelFile model = models().orientable(
                    name(block.get()),
                    modLoc("block/bus_side"),
                    modLoc("block/" + name(block.get()) + "_front"),
                    modLoc("block/bus_side")
            );

            horizontalBlock(block.get(), model, DEFAULT_ANGLE_OFFSET);
            simpleBlockItem(block.get(), model);
        }
    }

    private String name(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block).getPath();
    }

    public void horizontalBlock(Block block, ModelFile model, int angleOffset) {
        getVariantBuilder(block)
                .forAllStatesExcept(state -> ConfiguredModel.builder()
                        .modelFile(model)
                        .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + angleOffset) % 360)
                        .build(),
                        BlockItemBus.TYPE
                );
    }
}
