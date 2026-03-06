package org.moshang.fantasystructure.client.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.client.render.StructurePreviewRenderer;

@Mod.EventBusSubscriber(modid = FantasyStructure.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            StructurePreviewRenderer.renderPreview(event.getPoseStack(), event.getCamera(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            StructurePreviewRenderer.onClientTick();
        }
    }
}
