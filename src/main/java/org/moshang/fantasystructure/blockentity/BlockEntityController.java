package org.moshang.fantasystructure.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;

public abstract class BlockEntityController extends BlockEntity {
    protected boolean formed = false;
    private StructurePattern pattern;
    private final ResourceLocation id;
    private int ticks = 0;

    public BlockEntityController(BlockEntityType<?> entityType,
                                 BlockPos pos, BlockState state,
                                 ResourceLocation patternId) {
        super(entityType, pos, state);
        this.id = patternId;
    }

    public void tick() {
        if(level == null || level.isClientSide) return;

        ticks++;
        if(ticks % 40 == 0) {
            checkStructure();
            if(formed) {
                System.out.println("结构完整");
            }
        }
    }

    protected void initPattern() {
        if (pattern == null && getLevel() != null && !getLevel().isClientSide) {
            this.pattern = BlueprintManager.getPattern(id);
        }
    }

    protected boolean checkStructure() {
        if (pattern == null) initPattern();
        if (pattern == null) return false;
        formed = pattern.matches(level, worldPosition);
        return formed;
    }

    public boolean getFormed() {
        return formed;
    }
    public StructurePattern getPattern() {
        return pattern;
    }
}
