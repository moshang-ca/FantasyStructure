package org.moshang.fantasystructure.client.render;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.client.scene.forge.WorldSceneRendererImpl;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.moshang.fantasystructure.api.blockentity.BlockEntityControllerBase;
import org.moshang.fantasystructure.data.BlockInfo;
import org.moshang.fantasystructure.util.PreviewDummyWorld;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings({"ConstantValue", "CallToPrintStackTrace"})
@OnlyIn(Dist.CLIENT)
public class StructurePreviewRenderer {
    private enum CacheState {
        UNUSED, COMPILING, COMPILED
    }

    public static boolean RENDER = false;

    @Getter(lazy = true)
    private static final VertexBuffer[] BUFFERS = initBuffers();
    private static PreviewDummyWorld LEVEL = null;
    @Nullable
    private static Thread THREAD = null;
    @Nullable
    private static Set<BlockPos> BLOCK_ENTITIES;
    private static final AtomicInteger PREVIEW_LEFT_TICK = new AtomicInteger(-1);
    private static final AtomicInteger PREVIEW_ERROR_LEFT_TICK = new AtomicInteger(-1);
    @Nullable
    private static BlockPos ERROR_POS = null;
    @Nullable
    private static BlockPos LAST_POS = null;
    private static int LAST_LAYER = -1;
    private static final AtomicReference<CacheState> CACHE_STATE = new AtomicReference<>(CacheState.UNUSED);

    @SuppressWarnings("resource")
    private static VertexBuffer[] initBuffers() {
        List<RenderType> layers = RenderType.chunkBufferLayers();
        VertexBuffer[] buffers = new VertexBuffer[layers.size()];
        for(int i = 0; i < layers.size(); i++) {
            buffers[i] = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
        return buffers;
    }

    public static void cleanPreview() {
        CACHE_STATE.set(CacheState.UNUSED);
        LAST_LAYER = -1;
        LAST_POS = null;
        PREVIEW_LEFT_TICK.set(-1);
        LEVEL = null;
        BLOCK_ENTITIES = null;
        RENDER = false;
    }

    public static void removePreview(BlockPos pos) {
        if(LAST_POS != null && LAST_POS.equals(pos)) {
            cleanPreview();
        }
    }

    public static void cleanError() {
        ERROR_POS = null;
        PREVIEW_ERROR_LEFT_TICK.set(-1);
    }

    /**
     * Show multiblock structure preview
     * @param controllerPos the pos of controller
     * @param controller the controller
     * @param duration the ticks of preview's duration
     */
    public static void showPreview(BlockPos controllerPos, BlockEntityControllerBase controller, int duration) {
        RENDER = true;

        LEVEL = new PreviewDummyWorld();
        CompletableFuture.supplyAsync(() -> {
            var patternInfo = controller.getPattern().blockPattern();
            Long2ObjectOpenHashMap<BlockInfo> worldPattern = new Long2ObjectOpenHashMap<>();

            for(var entry : patternInfo.long2ObjectEntrySet()) {
                if(Thread.currentThread().isInterrupted()) return null;
                BlockPos worldPos = controllerPos.offset(BlockPos.of(entry.getLongKey()));
                worldPattern.put(worldPos.asLong(), entry.getValue());
            }
            return worldPattern;
        }).thenAcceptAsync(worldPattern -> {
            if(worldPattern == null) return;

            if (LAST_POS != null && LAST_POS.equals(controllerPos)) {
                LAST_LAYER++;
                if (LAST_LAYER >= controller.getPattern().height()) {
                    LAST_LAYER = -1;
                }
            } else {
                LAST_LAYER = -1;
            }
            LAST_POS = controllerPos;

            Set<BlockPos> renderedBlocks = worldPattern.keySet().longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
            LEVEL.addBlocks(worldPattern);
            prepareBuffers(LEVEL, renderedBlocks, duration);
        }, Minecraft.getInstance());


    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void prepareBuffers(PreviewDummyWorld level, Set<BlockPos> renderedBlocks, int duration) {
        if(THREAD != null) {
            THREAD.interrupt();
        }

        CACHE_STATE.set(CacheState.COMPILING);
        getBUFFERS();

        THREAD = new Thread(() -> {
            try {
                var dispatcher = Minecraft.getInstance().getBlockRenderer();
                ModelBlockRenderer.enableCaching();
                PoseStack poseStack = new PoseStack();
                var randomSource = RandomSource.createNewThreadLocalInstance();

                List<RenderType> layers = RenderType.chunkBufferLayers();
                for (int i = 0; i < layers.size(); i++) {
                    if (Thread.interrupted()) return;
                    var layer = layers.get(i);
                    var buffer = new BufferBuilder(layer.bufferSize());
                    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

                    renderBlock(level, poseStack, dispatcher, layer,
                            new WorldSceneRenderer.VertexConsumerWrapper(buffer),
                            renderedBlocks, randomSource);

                    var builder = buffer.end();
                    var vertexBuffer = getBUFFERS()[i];

                    CompletableFuture.runAsync(() -> {
                        // upload to GPU
                        if (!vertexBuffer.isInvalid()) {
                            vertexBuffer.bind();
                            vertexBuffer.upload(builder);
                            VertexBuffer.unbind();
                        }
                    }, runnable -> RenderSystem.recordRenderCall(() -> {
                        runnable.run();
                        CACHE_STATE.set(CacheState.COMPILED);
                    }));
                }

                ModelBlockRenderer.clearCache();

                Set<BlockPos> poses = new HashSet<>();
                for (BlockPos pos : renderedBlocks) {
                    if (Thread.interrupted()) return;
                    var be = level.getBlockEntity(pos);
                    if (be != null && Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be) != null) {
                        poses.add(pos);
                    }
                }

                if (Thread.interrupted()) return;
                BLOCK_ENTITIES = poses;
                THREAD = null;
                PREVIEW_LEFT_TICK.set(duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        THREAD.start();
    }

    private static void renderBlock(PreviewDummyWorld level, PoseStack poseStack, BlockRenderDispatcher dispatcher,
                                    RenderType layer, WorldSceneRenderer.VertexConsumerWrapper wrapper,
                                    Set<BlockPos> renderedBlocks, RandomSource randomSource) {
        for(BlockPos pos : renderedBlocks) {
            if(Thread.interrupted()) return;
            BlockState blockState = level.getBlockState(pos);
            FluidState fluidState = blockState.getFluidState();
            Block block = blockState.getBlock();
            if(block == Blocks.AIR) continue;

            if(blockState.getRenderShape() != RenderShape.INVISIBLE &&
                WorldSceneRendererImpl.canRenderInLayer(dispatcher, blockState, pos, level, layer, randomSource)) {
                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                poseStack.translate(.5f, .5f, .5f);
                poseStack.scale(.8f, .8f, .8f);
                poseStack.translate(-.5f, -.5f, -.5f);

                level.setRenderFilter(p -> p.equals(pos));
                WorldSceneRendererImpl.renderBlocksForge(dispatcher, blockState, pos, level, poseStack, wrapper, randomSource, layer);
                level.setRenderFilter(p -> true);
                poseStack.popPose();
            }

            if(!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == layer) {
                wrapper.addOffset(pos.getX() - (pos.getX() & 15),
                                    pos.getY() - (pos.getY() & 15),
                                    pos.getZ() - (pos.getZ() & 15));
                dispatcher.renderLiquid(pos, level, wrapper, blockState, fluidState);
            }
            wrapper.clerOffset();
            wrapper.clearColor();
        }
    }

    public static void onClientTick() {
        if (PREVIEW_LEFT_TICK.get() > 0) {
            if (PREVIEW_LEFT_TICK.decrementAndGet() <= 0) {
                cleanPreview();
            }
        }
        if (PREVIEW_ERROR_LEFT_TICK.get() > 0) {
            if (PREVIEW_ERROR_LEFT_TICK.decrementAndGet() <= 0) {
                cleanError();
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static void renderTESR(PoseStack poseStack, float partialTicks) {
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        for(BlockPos pos : BLOCK_ENTITIES) {
            BlockEntity be = LEVEL.getBlockEntity(pos);
            if(be == null) continue;

            BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);

            if(renderer != null) {
                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                renderer.render(be, partialTicks, poseStack, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }
        buffers.endBatch();
    }

    private static void setupRenderState(RenderType layer) {
        layer.setupRenderState();

        if(layer == RenderType.translucent()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
    }

    private static void clearRenderState(RenderType layer) {
        layer.clearRenderState();
    }

    // render structure preview in world
    @SuppressWarnings("DataFlowIssue")
    public static void renderPreview(PoseStack poseStack, Camera camera, float partialTicks) {
        if(LEVEL == null || CACHE_STATE.get() != CacheState.COMPILED) return;

        poseStack.pushPose();
        Vec3 cameraPos = camera.getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        List<RenderType> layers = RenderType.chunkBufferLayers();

        for (int i = 0; i < layers.size(); i++) {
            RenderType layer = layers.get(i);

            if (layer == RenderType.translucent() && BLOCK_ENTITIES != null) {
                renderTESR(poseStack, partialTicks);
            }

            VertexBuffer vertexBuffer = getBUFFERS()[i];
            if (vertexBuffer == null || vertexBuffer.isInvalid() || vertexBuffer.getFormat() == null) continue;

            setupRenderState(layer);
            poseStack.pushPose();
            ShaderInstance shaderInstance = RenderSystem.getShader();
            for (int j = 0; j < 12; ++j) {
                int k = RenderSystem.getShaderTexture(j);
                shaderInstance.setSampler("Sampler" + j, k);
            }

            RenderSystem.setupShaderLights(shaderInstance);
            shaderInstance.apply();

            vertexBuffer.bind();
            vertexBuffer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shaderInstance);

            poseStack.popPose();
            shaderInstance.clear();

            VertexBuffer.unbind();
            clearRenderState(layer);
        }

        poseStack.popPose();
    }
}
