package org.moshang.fantasystructure.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.moshang.fantasystructure.api.blockentity.RendererBlockEntity;
import org.moshang.fantasystructure.registry.FSBlockEntities;

@Getter
public class BEStarCore extends RendererBlockEntity {
    @Setter
    private int coreColor = 0x4A90E2;
    @Setter
    private int edgeColor = 0x8BB9FF;
    @Setter
    private float brightness = 1.f;

    public BEStarCore(BlockPos pPos, BlockState pBlockState) {
        super(FSBlockEntities.STAR_CORE_BE.get(), pPos, pBlockState);
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
