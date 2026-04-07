package org.moshang.fantasystructure.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.moshang.fantasystructure.api.blockentity.RendererBlockEntity;
import org.moshang.fantasystructure.registry.FSBlockEntities;


public class BEStarCore extends RendererBlockEntity {
    public static final float Radius = 5.f;

    @Setter @Getter
    private int coreColor = 0x4A90E2;
    @Setter @Getter
    private int edgeColor = 0x8BB9FF;
    @Setter @Getter
    private float brightness = 1.f;
    private AABB renderBoundingBox;

    public BEStarCore(BlockPos pPos, BlockState pBlockState) {
        super(FSBlockEntities.STAR_CORE_BE.get(), pPos, pBlockState);
    }

    @Override
    public AABB getRenderBoundingBox() {
        if(renderBoundingBox == null) {
            double size = Radius * 2;
            renderBoundingBox = AABB.ofSize(worldPosition.getCenter(), size, size, size);
        }
        return renderBoundingBox;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        animationTime += .01f;
        rotationAngle += .02f;
    }

    @Override
    public void tick() {}
}
