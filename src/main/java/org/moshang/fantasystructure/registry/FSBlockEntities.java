package org.moshang.fantasystructure.registry;

import com.lowdragmc.lowdraglib.forge.PlatformImpl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.blockentity.BEStarCore;
import org.moshang.fantasystructure.blockentity.container.BEEnergyBus;
import org.moshang.fantasystructure.blockentity.container.BEFluidBus;
import org.moshang.fantasystructure.blockentity.container.BEItemBus;
import org.moshang.fantasystructure.blockentity.controller.BEAEStorageController;
import org.moshang.fantasystructure.blockentity.controller.BETestController;
import org.moshang.fantasystructure.blockentity.creative.BlockEntityCreativeEnergySource;

import java.util.Arrays;

public class FSBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, FantasyStructure.MODID
    );

    // Controller registry
    public static final RegistryObject<BlockEntityType<BETestController>> TEST_CONTROLLER_BE = register("test_controller", BETestController::new, FSBlocks.TEST_CONTROLLER);
    public static final RegistryObject<BlockEntityType<BEAEStorageController>> AE_STORAGE_CONTROLLER_BE;

    public static final RegistryObject<BlockEntityType<BEItemBus>> ITEM_BUS_BE = register("item_bus", BEItemBus::new, merge(FSBlocks.ITEM_INPUT_BUSES, FSBlocks.ITEM_OUTPUT_BUSES));
    public static final RegistryObject<BlockEntityType<BEEnergyBus>> ENERGY_BUS_BE = register("energy_bus", BEEnergyBus::new, merge(FSBlocks.ENERGY_INPUT_BUSES, FSBlocks.ENERGY_OUTPUT_BUSES));
    public static final RegistryObject<BlockEntityType<BEFluidBus>> FLUID_BUS_BE = register("fluid_bus", BEFluidBus::new, merge(FSBlocks.FLUID_INPUT_BUSES, FSBlocks.FLUID_OUTPUT_BUSES));

    public static final RegistryObject<BlockEntityType<BlockEntityCreativeEnergySource>> CREATIVE_ENERGY_SOURCE_BE = register("creative_energy_source", BlockEntityCreativeEnergySource::new, FSBlocks.CREATIVE_ENERGY_SOURCE);

    public static final RegistryObject<BlockEntityType<BEStarCore>> STAR_CORE_BE = register("star_core", BEStarCore::new, FSBlocks.STAR_CORE);

    static {
        if(PlatformImpl.isModLoaded("ae2")) {
            AE_STORAGE_CONTROLLER_BE = register("ae_storage_controller", BEAEStorageController::new, FSBlocks.AE_STORAGE_CONTROLLER);
        } else {
            AE_STORAGE_CONTROLLER_BE = null;
        }
    }

    private FSBlockEntities() {}

    @SuppressWarnings("DataFlowIssue")
    @SafeVarargs
    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryObject<? extends Block>... blocks) {
        return BLOCK_ENTITIES.register(
                name,
                () -> BlockEntityType.Builder
                        .of(factory, Arrays.stream(blocks).map(RegistryObject::get).toArray(Block[]::new))
                        .build(null)
        );
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static RegistryObject<? extends Block>[] merge(RegistryObject<? extends Block>[]... blockArrays) {
        return Arrays.stream(blockArrays)
                .flatMap(Arrays::stream)
                .toArray(RegistryObject[]::new);
    }
}
