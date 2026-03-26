package org.moshang.fantasystructure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;

import java.util.Arrays;

@SuppressWarnings("unused")
public class FSCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FantasyStructure.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FantasyStructure.MODID + ".main"))
                    .icon(() -> new ItemStack(FSBlocks.TEST_CONTROLLER.get()))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(FSBlocks.TEST_CONTROLLER.get());

                        pOutput.accept(FSItems.AUTO_BUILDER.get());
                    }).build()
    );

    public static final RegistryObject<CreativeModeTab> BUS_TAB = TABS.register("bus",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FantasyStructure.MODID + ".bus"))
                    .icon(() -> new ItemStack(FSBlocks.ITEM_INPUT_BUSES[0].get()))
                    .withTabsBefore(MAIN_TAB.getKey())
                    .displayItems((pParameters, pOutput) -> {
                        Arrays.stream(FSBlocks.ITEM_INPUT_BUSES).toList().forEach(item -> pOutput.accept(item.get()));
                        Arrays.stream(FSBlocks.ITEM_OUTPUT_BUSES).toList().forEach(item -> pOutput.accept(item.get()));
                        Arrays.stream(FSBlocks.ENERGY_INPUT_BUSES).toList().forEach(item -> pOutput.accept(item.get()));
                        Arrays.stream(FSBlocks.ENERGY_OUTPUT_BUSES).toList().forEach(item -> pOutput.accept(item.get()));
                        Arrays.stream(FSBlocks.FLUID_INPUT_BUSES).toList().forEach(item -> pOutput.accept(item.get()));
                        Arrays.stream(FSBlocks.FLUID_OUTPUT_BUSES).toList().forEach(item -> pOutput.accept(item.get()));
                    }).build()
    );

    private FSCreativeModeTabs() {}
}
