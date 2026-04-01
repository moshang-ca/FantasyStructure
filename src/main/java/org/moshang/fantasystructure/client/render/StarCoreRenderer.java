package org.moshang.fantasystructure.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.blockentity.BEStarCore;
import org.moshang.fantasystructure.util.Geometry;
import org.moshang.fantasystructure.util.MathUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@OnlyIn(Dist.CLIENT)
public class StarCoreRenderer implements BlockEntityRenderer<BEStarCore> {
    private static final ResourceLocation STAR_TEXTURE = FantasyStructure.id("textures/entity/star_core.png");
    private static final float SIZE = 10.f;
    private final static AtomicBoolean initialized = new AtomicBoolean(false);

    // VBO
    private static int vaoId = -1;
    private static int vboId = -1;
    private static int vertexCount = 0;
    private static CompletableFuture<Void> future = null;

    private static final AtomicReference<List<Geometry.Vertex>> CACHE = new AtomicReference<>(null);

    public StarCoreRenderer(BlockEntityRendererProvider.Context context) {
        cleanup();
        initVBO();
    }

    private void initVBO() {
        if(initialized.get() || future != null)
            return;

        future = CompletableFuture.supplyAsync(() -> {
            try {
                List<Geometry.Vertex> vertices = CACHE.get();
                if(vertices == null) {
                    vertices = Geometry.icosphere(SIZE / 2.f, 3);
                    CACHE.set(vertices);
                }
                return vertices;
            } catch (Exception e) {
                FantasyStructure.LOGGER.error("Failed to generate sphere vertices!", e);
                return null;
            }
        }).thenAcceptAsync(vertices -> {
            if(vertices == null) return;

            RenderSystem.recordRenderCall(() -> {
                try {
                    setupVBO(vertices);
                    initialized.set(true);
                } catch (Exception e) {
                    FantasyStructure.LOGGER.error("Failed to initialize VBO!", e);
                }
            });
        }, Minecraft.getInstance());
    }

    private void setupVBO(List<Geometry.Vertex> vertices) {
        if(vertices == null || vertices.isEmpty()) return;

        vertexCount = vertices.size();

        float[] vertexData = new float[vertices.size() * 8];
        for(int i = 0; i < vertices.size(); ++i) {
            var v = vertices.get(i);
            vertexData[i * 8] = v.x();
            vertexData[i * 8 + 1] = v.y();
            vertexData[i * 8 + 2] = v.z();
            vertexData[i * 8 + 3] = v.nx();
            vertexData[i * 8 + 4] = v.ny();
            vertexData[i * 8 + 5] = v.nz();
            vertexData[i * 8 + 6] = v.u();
            vertexData[i * 8 + 7] = v.v();
        }

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexData, GL15.GL_DYNAMIC_DRAW);

        // Position
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 8 * 4, 0);
        GL20.glEnableVertexAttribArray(0);

        // Normal
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 8 * 4, 3 * 4);
        GL20.glEnableVertexAttribArray(1);

        // UV
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 8 * 4, 6 * 4);
        GL20.glEnableVertexAttribArray(2);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        FantasyStructure.LOGGER.info("Star core VBO initialized with {} vertices", vertexCount);
    }

    public static void cleanup() {
        if (vaoId != -1) {
            GL30.glDeleteVertexArrays(vaoId);
            vaoId = -1;
        }
        if (vboId != -1) {
            GL15.glDeleteBuffers(vboId);
            vboId = -1;
        }
        vertexCount = 0;
        initialized.set(false);
        CACHE.set(null);
        if(future != null) {
            future.cancel(true);
            future = null;
        }
    }

    @Override
    public void render(BEStarCore pBlockEntity, float pPartialTick, @NotNull PoseStack pPoseStack,
                       @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if(pBlockEntity.getLevel() != null && pBlockEntity.getLevel().isClientSide) {
            pBlockEntity.clientTick();
        }

        if(!initialized.get()) {
            return;
        }

        var shader = ShaderLoader.getShader("star_core");
        if(shader == null) return;

        pPoseStack.pushPose();
        pPoseStack.translate(.5f, .5f, .5f);

        float rotation = pBlockEntity.getRotationAngle();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(rotation));

        shader.apply();

        try {
            setupUniforms(shader, pBlockEntity, pPartialTick, pPoseStack);
            renderWithVBO();
        } finally {
            shader.clear();
        }

        pPoseStack.popPose();
    }

    private void setupUniforms(ShaderInstance shader, BEStarCore star, float partialTick, PoseStack poseStack) {
        Matrix4f matrix = poseStack.last().pose();
        shader.safeGetUniform("ModelViewMat").set(matrix);
        shader.safeGetUniform("ProjMat").set(RenderSystem.getProjectionMatrix());

        float time = star.getAnimationTime() + partialTick;
        shader.safeGetUniform("time").set(time);

        float[] coreColor = MathUtil.colorToFloat3D(star.getCoreColor());
        float[] edgeColor = MathUtil.colorToFloat3D(star.getEdgeColor());
        shader.safeGetUniform("coreColor").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("edgeColor").set(edgeColor[0], edgeColor[1], edgeColor[2]);
    }

    private void renderWithVBO() {
        if(vaoId == -1 || vertexCount == 0) return;
        RenderSystem.assertOnRenderThread();

        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        RenderSystem.setShaderTexture(0, STAR_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int texId = Minecraft.getInstance().getTextureManager().getTexture(STAR_TEXTURE).getId();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        GL30.glBindVertexArray(currentVAO);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
