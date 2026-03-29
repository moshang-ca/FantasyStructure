package org.moshang.fantasystructure.api.blockentity;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class RendererBlockEntity extends BlockEntity {
    @Getter @DescSynced
    protected float animationTime = 0f;
    @Getter @DescSynced
    protected float rotationAngle = 0f;

    public RendererBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public abstract void tick();
    @OnlyIn(Dist.CLIENT)
    public abstract void clientTick();
}
