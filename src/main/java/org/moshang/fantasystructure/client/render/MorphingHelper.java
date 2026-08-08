package org.moshang.fantasystructure.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.block.FSOverlays;
import org.moshang.fantasystructure.api.block.IMorphingBlock;
import org.moshang.fantasystructure.helper.StructurePattern;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("deprecation")
public final class MorphingHelper {

    private MorphingHelper() {
    }

    @Nullable
    public static Block findDominantNeighbor(BlockGetter level, BlockPos pos,
                                             StructurePattern pattern, BlockPos controllerPos) {
        Map<Block, Integer> counts = new HashMap<>();
        Block best = null;
        int bestCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    if (!pattern.blockPattern().containsKey(neighbor.subtract(controllerPos).asLong())) continue;
                    Block block = level.getBlockState(neighbor).getBlock();
                    if (block == Blocks.AIR || block instanceof IMorphingBlock) continue;
                    int count = counts.merge(block, 1, Integer::sum);
                    if (count > bestCount) {
                        bestCount = count;
                        best = block;
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    public static Block[] findDominantPerFace(BlockGetter level, BlockPos pos,
                                              StructurePattern pattern, BlockPos controllerPos) {
        Block[] result = new Block[6];
        for (Direction dir : Direction.values()) {
            result[dir.ordinal()] = findDominantOnFace(level, pos, dir, pattern, controllerPos);
        }
        return result;
    }

    @Nullable
    private static Block findDominantOnFace(BlockGetter level, BlockPos pos, Direction face,
                                            StructurePattern pattern, BlockPos controllerPos) {
        Direction planeA;
        Direction planeB;
        switch (face) {
            case UP, DOWN -> { planeA = Direction.EAST; planeB = Direction.NORTH; }
            case NORTH, SOUTH -> { planeA = Direction.EAST; planeB = Direction.UP; }
            default -> { planeA = Direction.NORTH; planeB = Direction.UP; } // EAST, WEST
        }
        Block dominant = voteOnPlane(level, pos.relative(face), planeA, planeB, pattern, controllerPos);
        if (dominant == null) {
            dominant = voteOnPlane(level, pos, planeA, planeB, pattern, controllerPos);
        }
        return dominant;
    }

    @Nullable
    private static Block voteOnPlane(BlockGetter level, BlockPos center, Direction planeA, Direction planeB,
                                     StructurePattern pattern, BlockPos controllerPos) {
        int[][] offsets = {{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        Map<Block, Integer> counts = new HashMap<>();
        Block best = null;
        int bestCount = 0;
        for (int[] off : offsets) {
            BlockPos neighbor = center.relative(planeA, off[0]).relative(planeB, off[1]);
            if (!pattern.blockPattern().containsKey(neighbor.subtract(controllerPos).asLong())) continue;
            Block block = level.getBlockState(neighbor).getBlock();
            if (block == Blocks.AIR || block instanceof IMorphingBlock) continue;
            int count = counts.merge(block, 1, Integer::sum);
            if (count > bestCount) {
                bestCount = count;
                best = block;
            }
        }
        return best;
    }

    @Nullable
    @SuppressWarnings("removal")
    public static TextureAtlasSprite resolveSprite(Block block) {
        ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(block);
        if (registryName != null) {
            ResourceLocation textureLocation = new ResourceLocation(registryName.getNamespace(), "block/" + registryName.getPath());
            try {
                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .apply(textureLocation);
                if (sprite != null && sprite.contents().name().equals(textureLocation)) {
                    return sprite;
                }
            } catch (Exception ignored) {
                // fall through to particle icon
            }
        }
        try {
            return Minecraft.getInstance().getBlockRenderer()
                    .getBlockModel(block.defaultBlockState())
                    .getParticleIcon();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolves the block's overlay texture sprite.
     */
    @Nullable
    @SuppressWarnings("removal")
    public static TextureAtlasSprite resolveOverlaySprite(Block block) {
        ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(block);
        ResourceLocation overlayLocation = FSOverlays.get(block);
        if (overlayLocation == null && registryName != null) {
            ResourceLocation legacy = new ResourceLocation(registryName.getNamespace(), "block/" + registryName.getPath() + "_overlay");
            if (hasSprite(legacy)) {
                overlayLocation = legacy;
            } else {
                overlayLocation = new ResourceLocation(registryName.getNamespace(), "block/overlay/" + registryName.getPath());
            }
        }
        if (overlayLocation == null) {
            return null;
        }
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(overlayLocation);
            if (sprite != null && sprite.contents().name().equals(overlayLocation)) {
                return sprite;
            }
        } catch (Exception ignored) {
            // no overlay texture
        }
        return null;
    }

    /**
     * Resolves the block's overlay texture sprite used while formed (e.g. a lit screen).
     * <p>
     * Priority: a custom location registered via KubeJS {@code ControllerBuilder.overlayFormed(...)},
     * then the convention {@code namespace:block/overlay/<path>_formed}.
     * Returns {@code null} when no formed overlay exists.
     */
    @Nullable
    @SuppressWarnings("removal")
    public static TextureAtlasSprite resolveOverlayFormedSprite(Block block) {
        ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(block);
        ResourceLocation overlayFormed = FSOverlays.getFormed(block);
        if (overlayFormed == null && registryName != null) {
            overlayFormed = new ResourceLocation(registryName.getNamespace(), "block/overlay/" + registryName.getPath() + "_formed");
        }
        if (overlayFormed == null) {
            return null;
        }
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(overlayFormed);
            if (sprite != null && sprite.contents().name().equals(overlayFormed)) {
                return sprite;
            }
        } catch (Exception ignored) {
            // no formed overlay texture
        }
        return null;
    }

    private static boolean hasSprite(ResourceLocation location) {
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(location);
            return sprite != null && sprite.contents().name().equals(location);
        } catch (Exception e) {
            return false;
        }
    }

    public static void refreshModelData(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !level.isClientSide) return;
        be.requestModelDataUpdate();
        if (level instanceof ClientLevel clientLevel) {
            BlockPos pos = be.getBlockPos();
            clientLevel.setSectionDirtyWithNeighbors(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ()));
        }
    }
}
