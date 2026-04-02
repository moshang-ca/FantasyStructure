package org.moshang.fantasystructure.client.event;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.client.render.ShaderLoader;
import org.moshang.fantasystructure.client.render.StarCoreRenderer;
import org.moshang.fantasystructure.client.screen.ControllerScreen;
import org.moshang.fantasystructure.client.screen.EnergyBusScreen;
import org.moshang.fantasystructure.client.screen.FluidBusScreen;
import org.moshang.fantasystructure.client.screen.ItemBusScreen;
import org.moshang.fantasystructure.registry.FSBlockEntities;
import org.moshang.fantasystructure.registry.FSMenuType;

@Mod.EventBusSubscriber(modid = FantasyStructure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FantasyStructureClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(FSMenuType.CONTROLLER_MENU_TYPE.get(), ControllerScreen::new);
            MenuScreens.register(FSMenuType.ITEM_BUS_MENU_TYPE.get(), ItemBusScreen::new);
            MenuScreens.register(FSMenuType.ENERGY_BUS_MENU_TYPE.get(), EnergyBusScreen::new);
            MenuScreens.register(FSMenuType.FLUID_BUS_MENU_TYPE.get(), FluidBusScreen::new);

            BlockEntityRenderers.register(FSBlockEntities.STAR_CORE_BE.get(), StarCoreRenderer::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        ShaderLoader.loadShaders("star_core", event.getResourceProvider());
    }
}
