package org.moshang.fantasystructure.client.event;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.client.render.ShaderLoader;
import org.moshang.fantasystructure.client.render.StarCoreRenderer;
import org.moshang.fantasystructure.registry.FSBlockEntities;

@Mod.EventBusSubscriber(modid = FantasyStructure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FantasyStructureClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(FSBlockEntities.STAR_CORE_BE.get(), StarCoreRenderer::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        ShaderLoader.loadShaders("star_core", event.getResourceProvider());
    }
}
