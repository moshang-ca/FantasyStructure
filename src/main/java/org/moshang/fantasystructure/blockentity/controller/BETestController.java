package org.moshang.fantasystructure.blockentity.controller;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.blockentity.BlockEntityControllerBase;
import org.moshang.fantasystructure.registry.FSBlockEntities;
import org.slf4j.Logger;

@SuppressWarnings("removal")
public class BETestController extends BlockEntityControllerBase {
    private static final Logger LOGGER = LogUtils.getLogger();

    public BETestController(BlockPos pos, BlockState state) {
        this(
                FSBlockEntities.TEST_CONTROLLER_BE.get(),
                pos, state,
                new ResourceLocation(FantasyStructure.MODID, "test_structure")
        );
    }

    public BETestController(BlockEntityType<?> entityType, BlockPos pos,
                            BlockState state, ResourceLocation id) {
        super(entityType, pos, state, id);
    }

    public void getPatternInfo() {
        getPattern().blockPattern().forEach((pos, info) -> System.out.printf("pos %s is %s%n", pos, info.getExpectedState()));
    }


}
