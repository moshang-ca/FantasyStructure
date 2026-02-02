package org.moshang.fantasystructure.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.menu.BusMenu;
import org.moshang.fantasystructure.menu.ControllerMenu;

public class FSMenuType {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES, FantasyStructure.MODID
    );

    public static final RegistryObject<MenuType<ControllerMenu>> CONTROLLER_MENU_TYPE = MENU_TYPES.register("controller_menu", () -> IForgeMenuType.create(ControllerMenu::new));
    public static final RegistryObject<MenuType<BusMenu>> BUS_MENU_TYPE = MENU_TYPES.register("bus_menu", () -> IForgeMenuType.create(BusMenu::new));
}
