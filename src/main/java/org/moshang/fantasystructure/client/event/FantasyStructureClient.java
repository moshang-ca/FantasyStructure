package org.moshang.fantasystructure.client.event;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.block.IMorphingBlock;
import org.moshang.fantasystructure.client.render.ShaderLoader;
import org.moshang.fantasystructure.client.render.StarCoreRenderer;
import org.moshang.fantasystructure.registry.FSBlockEntities;

@Mod.EventBusSubscriber(modid = FantasyStructure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FantasyStructureClient {
    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(FSBlockEntities.STAR_CORE_BE.get(), StarCoreRenderer::new);
            for (Block block : ForgeRegistries.BLOCKS.getValues()) {
                if (block instanceof IMorphingBlock) {
                    ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
                }
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        ShaderLoader.loadShaders("star_core", event.getResourceProvider());
    }
}
