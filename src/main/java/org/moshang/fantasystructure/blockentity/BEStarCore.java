package org.moshang.fantasystructure.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.moshang.fantasystructure.api.blockentity.RendererBlockEntity;
import org.moshang.fantasystructure.registry.FSBlockEntities;

@Getter
public class BEStarCore extends RendererBlockEntity {
    private float pulseIntensity = 1.f;
    private float glowStrength = 1.f;

    @Setter
    private int coreColor = 0xFFD966;
    @Setter
    private int glowColor = 0xFF8844;

    @Setter
    private float glowRadius = .8f;
    @Setter
    private float brightness = 1.f;
    @Setter
    private float pulseSpeed = 1.f;

    public BEStarCore(BlockPos pPos, BlockState pBlockState) {
        super(FSBlockEntities.STAR_CORE_BE.get(), pPos, pBlockState);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        animationTime += .01f;
        pulseIntensity = .8f + .4f * Mth.sin(animationTime * pulseSpeed * 3.f);
    }

    @Override
    public void tick() {
        if(level == null || level.isClientSide) return;

        animationTime += .05f;
        rotationAngle += .02f;

        pulseIntensity = .8f + .4f * Mth.sin(animationTime * pulseSpeed * 3.f);

        if(level.getGameTime() % 20 == 0) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
