package org.moshang.fantasystructure.client.render;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.client.model.MorphingModelData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MorphingBakedModel implements IDynamicBakedModel {
    private final BakedModel base;
    private static boolean morphFaceLogged = false;

    public MorphingBakedModel(BakedModel base) {
        this.base = base;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData data,
                                    @Nullable RenderType renderType) {
        MorphingModelData.MorphingData morph = data.get(MorphingModelData.PROPERTY);
        if (morph != null && morph.faceSprites() != null && !morphFaceLogged) {
            morphFaceLogged = true;
        }
        List<BakedQuad> quads = base.getQuads(state, side, rand, data, renderType);

        if (morph != null && morph.formed() && !quads.isEmpty()) {
            List<BakedQuad> remapped = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                TextureAtlasSprite faceSprite = null;
                Direction dir = quad.getDirection();
                if (morph.faceSprites() != null) {
                    faceSprite = morph.faceSprites()[dir.ordinal()];
                }
                if (faceSprite == null) {
                    faceSprite = morph.sprite();
                }
                if (faceSprite == null || quad.getSprite() == faceSprite) {
                    remapped.add(quad);
                    continue;
                }
                remapped.add(remapQuad(quad, faceSprite));
            }
            quads = remapped;
        }

        TextureAtlasSprite overlay = null;
        if (morph != null) {
            overlay = morph.formed() && morph.overlayFormedSprite() != null
                    ? morph.overlayFormedSprite() : morph.overlaySprite();
        }
        if (overlay != null) {
            List<BakedQuad> overlayQuads = new ArrayList<>();
            for (BakedQuad quad : base.getQuads(state, side, rand, data, renderType)) {
                overlayQuads.add(remapQuad(quad, overlay));
            }
            if (!overlayQuads.isEmpty()) {
                List<BakedQuad> combined = new ArrayList<>(quads.size() + overlayQuads.size());
                combined.addAll(quads);
                combined.addAll(overlayQuads);
                quads = combined;
            }
        }
        return quads;
    }

    private static BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite newSprite) {
        int[] oldVerts = quad.getVertices();
        int[] newVerts = oldVerts.clone();
        Direction dir = quad.getDirection();
        for (int i = 0; i < newVerts.length; i += 8) {
            float x = Float.intBitsToFloat(newVerts[i]);
            float y = Float.intBitsToFloat(newVerts[i + 1]);
            float z = Float.intBitsToFloat(newVerts[i + 2]);
            float u;
            float v;
            switch (dir) {
                case EAST, WEST -> { u = z; v = y; }
                case UP, DOWN -> { u = x; v = z; }
                default -> { u = x; v = y; }
            }
            newVerts[i + 4] = Float.floatToRawIntBits(newSprite.getU(Mth.clamp(u * 16.0f, 0.0f, 16.0f)));
            newVerts[i + 5] = Float.floatToRawIntBits(newSprite.getV(Mth.clamp(v * 16.0f, 0.0f, 16.0f)));
        }
        return new BakedQuad(newVerts, quad.getTintIndex(), quad.getDirection(),
                newSprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return base.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return base.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return base.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() { return base.getParticleIcon(); }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return base.getParticleIcon(data);
    }

    @Override
    public ItemTransforms getTransforms() { return base.getTransforms(); }

    @Override
    public ItemOverrides getOverrides() {
        return base.getOverrides();
    }
}
