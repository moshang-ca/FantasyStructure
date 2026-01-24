package org.moshang.fantasystructure.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.helper.builder.StructureBuilderManager;
import org.slf4j.Logger;

public abstract class BlockEntityControllerBase extends BlockEntity {
    protected boolean formed = false;
    private StructurePattern pattern;
    private final ResourceLocation id;
    private int ticks = 0;

    private static final Logger LOGGER = LogUtils.getLogger();

    public BlockEntityControllerBase(BlockEntityType<?> entityType,
                                     BlockPos pos, BlockState state,
                                     ResourceLocation patternId) {
        super(entityType, pos, state);
        this.id = patternId;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            initPattern();
            checkStructure();
        }
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

    public void autoBuild(ItemStack builderStack, boolean isCreative) {
        if(pattern == null) initPattern();
        LOGGER.info("autoBuild");
        StructureBuilderManager.startBuild(level, worldPosition, this.pattern, builderStack, isCreative);
    }

    public boolean getFormed() {
        return formed;
    }
    public StructurePattern getPattern() {
        return pattern;
    }

    @Override
    public void load(CompoundTag p_155245_) {
        super.load(p_155245_);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }
}
