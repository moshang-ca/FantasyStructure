package org.moshang.fantasystructure.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.api.blockentity.BERecipeControllerKjs;

import java.util.function.Supplier;

/**
 * KubeJS dynamic recipe controller block.
 * Each instance created via KubeJS script gets its own controllerId, baseParallel, parallelLimit, and maxThreads.
 * The BlockEntityType is provided lazily by KubeJS's registration system via the supplier.
 */
public class BlockRecipeControllerKjs extends BlockAbstractController<BERecipeControllerKjs> {

    private final ResourceLocation controllerId;
    private final int baseParallel;
    private final int parallelLimit;
    private final int maxThreads;

    /**
     * Constructs a KJS recipe controller block.
     *
     * @param strength        block hardness (from BlockBuilder.hardness)
     * @param beTypeSupplier  supplier for the BlockEntityType (set after registration by ControllerBuilder)
     * @param controllerId    the controller ID used to look up StructureDefinition
     * @param baseParallel    base parallel count
     * @param parallelLimit   maximum parallel count (-1 = unlimited)
     * @param maxThreads      number of recipe processing threads
     */
    public BlockRecipeControllerKjs(int strength,
                                    Supplier<BlockEntityType<BERecipeControllerKjs>> beTypeSupplier,
                                    ResourceLocation controllerId,
                                    int baseParallel,
                                    int parallelLimit,
                                    int maxThreads) {
        super(strength, beTypeSupplier, () -> controllerId);
        this.controllerId = controllerId;
        this.baseParallel = baseParallel;
        this.parallelLimit = parallelLimit;
        this.maxThreads = maxThreads;
    }

    @Override
    @NotNull
    protected BERecipeControllerKjs createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BERecipeControllerKjs(
                getBlockEntityTypeSupplier().get(),
                pos, state,
                controllerId,
                baseParallel, parallelLimit, maxThreads
        );
    }
}
