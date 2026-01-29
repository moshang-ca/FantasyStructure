package org.moshang.fantasystructure.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.registry.FSBlocks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FSBlockTagProvider extends BlockTagsProvider {
    public FSBlockTagProvider(
            PackOutput packOutput,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, FantasyStructure.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        addEffectiveTools();
    }

    private static final RegistryObject<?>[] SPECIAL_CONTROLLER = {

    };

    private void addEffectiveTools() {
        Map<RegistryObject<?>, List<TagKey<Block>>> specialTools = new HashMap<>();
        for(var specialController : SPECIAL_CONTROLLER) {
            specialTools.put(specialController, List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL));
        }
        var defaultTags = List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL);

        for(var block : FSBlocks.getBlocks()) {
            for(var desireTool : specialTools.getOrDefault(block, defaultTags)) {
                tag(desireTool).add((Block)(block.get()));
            }
        }
    }
}
