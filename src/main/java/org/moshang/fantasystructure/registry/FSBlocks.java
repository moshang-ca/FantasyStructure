package org.moshang.fantasystructure.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.block.controller.BlockTestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class FSBlocks {
    private static final List<RegistryObject<?>> AllBlocks = new ArrayList<>();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, FantasyStructure.MODID
    );

    public static final RegistryObject<Block> TEST_CONTROLLER = register("test_controller", () -> new BlockTestController(3));

    private FSBlocks() {}

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
        return register(name, block, 64);
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, int stackTo) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);

        FSItems.ITEMS.register(
                name, () -> new BlockItem(
                        toReturn.get(), new Item.Properties().stacksTo(stackTo)
                )
        );

        AllBlocks.add(toReturn);
        return toReturn;
    }

    public static List<RegistryObject<?>> getBlocks() {
        return Collections.unmodifiableList(AllBlocks);
    }
}
