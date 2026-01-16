package org.moshang.fantasystructure;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.developed.BlockTestController;
import org.moshang.fantasystructure.developed.BETestController;

public class FSRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, FantasyStructure.MODID
    );

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, FantasyStructure.MODID
    );

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, FantasyStructure.MODID
    );

    public static final RegistryObject<Block> TEST_CONTROLLER = BLOCKS.register(
            "test_controller",
            () -> new BlockTestController(
                    Block.Properties.of()
                            .strength(3.f)
                            .requiresCorrectToolForDrops()
            )
    );

    public static final RegistryObject<BlockEntityType<BETestController>> TEST_CONTROLLER_BE =
            BLOCK_ENTITIES.register(
                    "test_controller",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new BETestController(
                                    // 这里传入null，实际类型会在注册时由Forge注入
                                    null, pos, state, null
                            ),
                            TEST_CONTROLLER.get()
                    ).build(null)
            );

    public static final RegistryObject<Item> TEST_CONTROLLER_ITEM = ITEMS.register(
            "test_controller",
            () -> new BlockItem(
                    TEST_CONTROLLER.get(),
                    new Item.Properties()
            )
    );
}
