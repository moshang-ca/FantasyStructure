package org.moshang.fantasystructure;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.moshang.fantasystructure.api.recipe.FSRecipeSerializer;
import org.moshang.fantasystructure.api.recipe.ingredient.SizedIngredient;
import org.moshang.fantasystructure.command.Command;
import org.moshang.fantasystructure.helper.blueprint.BlueprintEditor;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.helper.builder.StructureBuilderManager;
import org.moshang.fantasystructure.registry.*;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;
import org.slf4j.Logger;

import java.util.Random;

@SuppressWarnings("removal")
@Mod(FantasyStructure.MODID)
public class FantasyStructure {
    public static final String MODID = "fantasystructure";
    public static final Random RND = new Random();
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasyStructure() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerRecipeTypes);

        init(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(StructureBuilderManager.class);
        MinecraftForge.EVENT_BUS.addListener(this::commandRegister);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        CraftingHelper.register(SizedIngredient.TYPE, SizedIngredient.SERIALIZER);
    }

    private void registerRecipeTypes(RegisterEvent event) {
        if(event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_TYPES)) {
            FSRecipes.RECIPE_TYPES.forEach(recipeType ->
                    ForgeRegistries.RECIPE_TYPES.register(recipeType.getRegistryName(), recipeType));
        } else if(event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
            FSRecipes.RECIPE_TYPES.forEach(recipeType ->
                    ForgeRegistries.RECIPE_SERIALIZERS.register(recipeType.getRegistryName(), new FSRecipeSerializer()));
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlueprintManager.init(FMLPaths.CONFIGDIR.get());
            BlueprintEditor.init();
        });

        FSRecipes.RECIPE_TYPES.freeze();
        FSRecipes.RECIPE_CAPABILITIES.freeze();
        FSStructureDefinitions.DEFINITIONS.freeze();
    }

    private void init(IEventBus modEventBus) {
        FSBlocks.BLOCKS.register(modEventBus);
        FSBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        FSItems.ITEMS.register(modEventBus);
        FSCreativeModeTabs.TABS.register(modEventBus);
    }

    private void commandRegister(RegisterCommandsEvent event) {
        Command.register(event.getDispatcher());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
