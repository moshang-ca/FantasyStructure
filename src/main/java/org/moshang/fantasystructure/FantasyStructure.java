package org.moshang.fantasystructure;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.moshang.fantasystructure.api.recipe.ingredient.SizedIngredient;
import org.moshang.fantasystructure.client.screen.ControllerScreen;
import org.moshang.fantasystructure.client.screen.EnergyBusScreen;
import org.moshang.fantasystructure.client.screen.ItemBusScreen;
import org.moshang.fantasystructure.command.Command;
import org.moshang.fantasystructure.helper.blueprint.BlueprintEditor;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.helper.builder.StructureBuilderManager;
import org.moshang.fantasystructure.network.FSMessages;
import org.moshang.fantasystructure.registry.FSBlockEntities;
import org.moshang.fantasystructure.registry.FSBlocks;
import org.moshang.fantasystructure.registry.FSItems;
import org.moshang.fantasystructure.registry.FSMenuType;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;
import org.slf4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@SuppressWarnings("removal")
@Mod(FantasyStructure.MODID)
public class FantasyStructure {
    public static final String MODID = "fantasystructure";
    public static final Random RND = new Random();
    private static final Logger LOGGER = LogUtils.getLogger();

    public FantasyStructure() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        init(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(StructureBuilderManager.class);
        MinecraftForge.EVENT_BUS.addListener(this::commandRegister);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        CraftingHelper.register(SizedIngredient.TYPE, SizedIngredient.SERIALIZER);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FSMessages.register();
            BlueprintManager.init(FMLPaths.CONFIGDIR.get());
            BlueprintEditor.init();
        });
    }

    private void init(IEventBus modEventBus) {
        FSMenuType.MENU_TYPES.register(modEventBus);
        FSBlocks.BLOCKS.register(modEventBus);
        FSBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        FSItems.ITEMS.register(modEventBus);

        FSMenuType.registerMenuFactories();
        FSRecipes.initRecipeCapabilities();
        FSRecipes.initRecipeTypes();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    public void commandRegister(RegisterCommandsEvent event) {
        Command.register(event.getDispatcher());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(FSMenuType.CONTROLLER_MENU_TYPE.get(), ControllerScreen::new);
                MenuScreens.register(FSMenuType.ITEM_BUS_MENU_TYPE.get(), ItemBusScreen::new);
                MenuScreens.register(FSMenuType.ENERGY_BUS_MENU_TYPE.get(), EnergyBusScreen::new);
            });
        }
    }
}
