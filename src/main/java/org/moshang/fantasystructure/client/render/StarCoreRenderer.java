package org.moshang.fantasystructure.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.blockentity.BEStarCore;
import org.moshang.fantasystructure.util.Geometry;
import org.moshang.fantasystructure.util.MathUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class StarCoreRenderer implements BlockEntityRenderer<BEStarCore> {
    private static final ResourceLocation STAR_TEXTURE = FantasyStructure.id("textures/entity/star_core.png");
    private static final float SIZE = 5.f;
    private final static AtomicBoolean initialized = new AtomicBoolean(false);

    // VBO
    private static int vaoId = -1;
    private static int vboId = -1;
    private static int vertexCount = 0;
    private static CompletableFuture<Void> future = null;

    private static List<Geometry.Vertex> CACHE = null;

    public StarCoreRenderer(BlockEntityRendererProvider.Context context) {
        cleanup();
        initVBO();
    }

    private void initVBO() {
        if(initialized.get() || future != null)
            return;

        future = CompletableFuture.runAsync(() -> {
            try {
                List<Geometry.Vertex> vertices;
                if(CACHE != null) {
                    vertices = CACHE;
                } else {
                    vertices = Geometry.smoothSphere(SIZE / 2.f, 3);
                    CACHE = vertices;
                }

                Minecraft.getInstance().execute(() -> {
                    try {
                        setupVBO(vertices);
                        initialized.set(true);
                    } catch (Exception e) {
                        FantasyStructure.LOGGER.error("Failed to setup VBO!", e);
                    }
                });
            } catch (Exception e) {
                FantasyStructure.LOGGER.error("Failed to generate sphere vertices!", e);
            }
        });
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
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexData, GL15.GL_STATIC_DRAW);

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
        CACHE = null;
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

        var shader = ShaderLoader.getInstance().getShader();
        if(shader == null) return;

        pPoseStack.pushPose();
        pPoseStack.translate(.5f, .5f, .5f);

        float rotation = pBlockEntity.getRotationAngle();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(rotation));

        float scale = 0.8f + .2f * pBlockEntity.getPulseIntensity();
        pPoseStack.scale(scale, scale, scale);

        shader.apply();

        try {
            setupUniforms(shader, pBlockEntity, pPartialTick);
            renderWithVBO(pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
        } finally {
            shader.clear();
        }

        pPoseStack.popPose();
    }

    private void setupUniforms(ShaderInstance shader, BEStarCore star, float partialTick) {
        float time = star.getAnimationTime() + partialTick;
        shader.safeGetUniform("time").set(time);

        float pulseIntensity = star.getPulseIntensity();
        shader.safeGetUniform("pulseIntensity").set(pulseIntensity);

        float[] coreColor = MathUtil.colorToFloat3D(star.getCoreColor());
        float[] glowColor = MathUtil.colorToFloat3D(star.getGlowColor());
        shader.safeGetUniform("coreColor").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("glowColor").set(glowColor[0], glowColor[1], glowColor[2]);

        shader.safeGetUniform("glowRadius").set(star.getGlowRadius());
        shader.safeGetUniform("brightness").set(star.getBrightness());

        var window = Minecraft.getInstance().getWindow();
        shader.safeGetUniform("resolution").set(new float[] { window.getWidth(), window.getHeight() });
    }

    private void renderWithVBO(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if(vaoId == -1 || vertexCount == 0) return;

        RenderSystem.setShaderTexture(0, STAR_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        
        var shader = ShaderLoader.getInstance().getShader();
        if(shader != null) {
            shader.safeGetUniform("ModelViewMat").set(matrix);
            shader.safeGetUniform("ProjMat").set(RenderSystem.getProjectionMatrix());
        }

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        GL30.glBindVertexArray(0);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderWithBuffer(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        RenderType renderType = RenderType.LINES;
        VertexConsumer consumer = buffer.getBuffer(renderType);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        List<Geometry.Vertex> vertices = CACHE;
        if (vertices == null) return;

        RenderSystem.lineWidth(2.0f);

        for (int i = 0; i < vertices.size(); i += 3) {
            var v1 = vertices.get(i);
            var v2 = vertices.get(i + 1);
            var v3 = vertices.get(i + 2);

            float r, g, b;
            r = 1.0f; g = 1.0f; b = 0.2f;

            consumer.vertex(matrix, v1.x(), v1.y(), v1.z())
                    .color(r, g, b, 1.0f)
                    .uv2(light)
                    .normal(normal, v1.nx(), v1.ny(), v1.nz())
                    .endVertex();
            consumer.vertex(matrix, v2.x(), v2.y(), v2.z())
                    .color(r, g, b, 1.0f)
                    .uv2(light)
                    .normal(normal, v2.nx(), v2.ny(), v2.nz())
                    .endVertex();

            consumer.vertex(matrix, v2.x(), v2.y(), v2.z())
                    .color(r, g, b, 1.0f)
                    .uv2(light)
                    .normal(normal, v2.nx(), v2.ny(), v2.nz())
                    .endVertex();
            consumer.vertex(matrix, v3.x(), v3.y(), v3.z())
                    .color(r, g, b, 1.0f)
                    .uv2(light)
                    .normal(normal, v3.nx(), v3.ny(), v3.nz())
                    .endVertex();

            consumer.vertex(matrix, v3.x(), v3.y(), v3.z())
                    .color(r, g, b, 1.0f)
                    .uv2(light)
                    .normal(normal, v3.nx(), v3.ny(), v3.nz())
                    .endVertex();
            consumer.vertex(matrix, v1.x(), v1.y(), v1.z())
                    .color(r, g, b, 1.0f)
                    .uv2(light)
                    .normal(normal, v1.nx(), v1.ny(), v1.nz())
                    .endVertex();
        }

        RenderSystem.lineWidth(1.0f);
    }
}
