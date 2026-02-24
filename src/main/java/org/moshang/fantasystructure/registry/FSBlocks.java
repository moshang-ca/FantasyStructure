package org.moshang.fantasystructure.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.capacity.ComponentEnergyCapacity;
import org.moshang.fantasystructure.api.capacity.ComponentFluidCapacity;
import org.moshang.fantasystructure.api.capacity.ComponentItemCapacity;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.block.container.BlockEnergyBus;
import org.moshang.fantasystructure.block.container.BlockFluidBus;
import org.moshang.fantasystructure.block.container.BlockItemBus;
import org.moshang.fantasystructure.block.controller.BlockTestController;
import org.moshang.fantasystructure.block.creative.BlockCreativeEnergySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class FSBlocks {
    private static final List<RegistryObject<?>> AllBlocks = new ArrayList<>();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, FantasyStructure.MODID
    );

    public static final RegistryObject<Block> TEST_CONTROLLER = register("test_controller", () -> new BlockTestController(3));

    public static final RegistryObject<Block>[] ITEM_INPUT_BUSES = List.of(
            register("tiny_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.TINY, IO.IN)),
            register("small_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.SMALL, IO.IN)),
            register("medium_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.MEDIUM, IO.IN)),
            register("large_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.LARGE, IO.IN)),
            register("great_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.GREAT, IO.IN)),
            register("giant_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.GIANT, IO.IN)),
            register("colossal_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.COLOSSAL, IO.IN)),
            register("titanic_item_input_bus", () -> new BlockItemBus(3, ComponentItemCapacity.TITANIC, IO.IN))
    ).toArray(RegistryObject[]::new);
    public static final RegistryObject<Block>[] ITEM_OUTPUT_BUSES = List.of(
            register("tiny_item_output_bus", () -> new BlockItemBus(3, ComponentItemCapacity.TINY, IO.OUT)),
            register("small_item_output_bus", () -> new BlockItemBus(3, ComponentItemCapacity.SMALL, IO.OUT))
    ).toArray(RegistryObject[]::new);
    public static final RegistryObject<Block>[] ENERGY_INPUT_BUSES = List.of(
            register("tiny_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.TINY, IO.IN)),
            register("small_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.SMALL, IO.IN)),
            register("medium_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.MEDIUM, IO.IN)),
            register("large_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.LARGE, IO.IN)),
            register("great_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.GREAT, IO.IN)),
            register("giant_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.GIANT, IO.IN)),
            register("colossal_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.COLOSSAL, IO.IN)),
            register("titanic_energy_input_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.TITANIC, IO.IN))
    ).toArray(RegistryObject[]::new);
    public static final RegistryObject<Block>[] ENERGY_OUTPUT_BUSES = List.of(
            register("tiny_energy_output_bus", () -> new BlockEnergyBus(3, ComponentEnergyCapacity.TINY, IO.OUT))
    ).toArray(RegistryObject[]::new);
    public static final RegistryObject<Block>[] FLUID_INPUT_BUSES = List.of(
            register("tiny_fluid_input_bus", () -> new BlockFluidBus(3, ComponentFluidCapacity.TINY, IO.IN))
    ).toArray(RegistryObject[]::new);
    public static final RegistryObject<Block>[] FLUID_OUTPUT_BUSES = List.of(
            register("tiny_fluid_output_bus", () -> new BlockFluidBus(3, ComponentFluidCapacity.TINY, IO.IN))
    ).toArray(RegistryObject[]::new);

    public static final RegistryObject<Block> CREATIVE_ENERGY_SOURCE = register("creative_energy_source", () -> new BlockCreativeEnergySource(3));

    private FSBlocks() {}

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
        return register(name, block, 64);
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, int stackTo) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);

        FSItems.ITEMS.register(name,
                () -> new BlockItem(toReturn.get(), new Item.Properties().stacksTo(stackTo)));

        AllBlocks.add(toReturn);
        return toReturn;
    }
    public static List<RegistryObject<?>> getBlocks() {
        return Collections.unmodifiableList(AllBlocks);
    }
}
