package org.moshang.fantasystructure.client.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public class MorphingModelData {
    public static final ModelProperty<MorphingData> PROPERTY = new ModelProperty<>();

    /**
     * @param formed             whether the structure is currently formed
     * @param sprite             fallback base sprite (overall dominant surrounding block),
     *                           or {@code null} when not formed / no suitable neighbour found
     * @param faceSprites        per-face base sprites indexed by {@link Direction#ordinal()};
     *                           a {@code null} entry falls back to {@code sprite}. May be {@code null}
     *                           when per-face morphing is not used.
     * @param overlaySprite      the block's overlay texture sprite (always rendered on top, unchanged),
     *                           or {@code null} when the block has no overlay texture
     * @param overlayFormedSprite the block's overlay sprite used while formed,
     *                           or {@code null} to keep using {@code overlaySprite}
     */
    public record MorphingData(boolean formed,
                               @Nullable TextureAtlasSprite sprite,
                               @Nullable TextureAtlasSprite[] faceSprites,
                               @Nullable TextureAtlasSprite overlaySprite,
                               @Nullable TextureAtlasSprite overlayFormedSprite) {
    }

    public static ModelData build(boolean formed,
                                  @Nullable TextureAtlasSprite sprite,
                                  @Nullable TextureAtlasSprite[] faceSprites,
                                  @Nullable TextureAtlasSprite overlaySprite,
                                  @Nullable TextureAtlasSprite overlayFormedSprite) {
        return ModelData.builder().with(PROPERTY,
                new MorphingData(formed, sprite, faceSprites, overlaySprite, overlayFormedSprite)).build();
    }
}
