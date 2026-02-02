package org.moshang.fantasystructure.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.blockentity.container.BEItemInputBus;
import org.moshang.fantasystructure.blockentity.controller.BETestController;

import java.util.Arrays;

public class FSBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, FantasyStructure.MODID
    );

    public static final RegistryObject<BlockEntityType<BETestController>> TEST_CONTROLLER_BE = register("test_controller", BETestController::new, FSBlocks.TEST_CONTROLLER);
    public static final RegistryObject<BlockEntityType<BEItemInputBus>> ITEM_INPUT_BUS_BE = register("tiny_item_input_bus", BEItemInputBus::new, FSBlocks.ITEM_INPUT_BUSES);

    private FSBlockEntities() {}

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
}
