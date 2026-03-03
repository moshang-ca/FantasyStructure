package org.moshang.fantasystructure.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.FantasyStructure;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("removal")
public class FSFluidTagProvider extends FluidTagsProvider {
    public static final TagKey<Fluid> FORGE_WATER = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), new ResourceLocation("forge", "water"));
    public static final TagKey<Fluid> FORGE_LAVA = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), new ResourceLocation("forge", "lava"));

    public static final TagKey<Fluid> FORGE_MILK = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), new ResourceLocation("forge", "milk"));

    public FSFluidTagProvider(
            PackOutput packOutput,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, FantasyStructure.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(FORGE_WATER).add(Fluids.WATER).add(Fluids.FLOWING_WATER);
        tag(FORGE_LAVA).add(Fluids.LAVA).add(Fluids.FLOWING_LAVA);
    }
}
