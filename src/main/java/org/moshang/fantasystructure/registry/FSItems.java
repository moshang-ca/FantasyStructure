package org.moshang.fantasystructure.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.item.ItemAutoBuilder;

public class FSItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, FantasyStructure.MODID
    );

    public static final RegistryObject<Item> AUTO_BUILDER = ITEMS.register("auto_builder", () -> new ItemAutoBuilder(1));

    private FSItems() {}
}
