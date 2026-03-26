package org.moshang.fantasystructure.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.menu.*;

public class FSMenuType {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES, FantasyStructure.MODID
    );

    public static final RegistryObject<MenuType<ControllerMenu>> CONTROLLER_MENU_TYPE = MENU_TYPES.register("controller_menu", () -> IForgeMenuType.create(ControllerMenu::new));
    public static final RegistryObject<MenuType<ItemBusMenu>> ITEM_BUS_MENU_TYPE = MENU_TYPES.register("item_bus_menu", () -> IForgeMenuType.create(ItemBusMenu::new));
    public static final RegistryObject<MenuType<EnergyBusMenu>> ENERGY_BUS_MENU_TYPE = MENU_TYPES.register("energy_bus_menu", () -> IForgeMenuType.create(EnergyBusMenu::new));
    public static final RegistryObject<MenuType<FluidBusMenu>> FLUID_BUS_MENU_TYPE = MENU_TYPES.register("fluid_bus_menu", () -> IForgeMenuType.create(FluidBusMenu::new));

    private FSMenuType() {}

    public static void registerMenuFactories() {
        BaseMenu.register(ControllerMenu.class, ControllerMenu::createForServer);
        BaseMenu.register(ItemBusMenu.class, ItemBusMenu::createForServer);
        BaseMenu.register(EnergyBusMenu.class, EnergyBusMenu::createForServer);
        BaseMenu.register(FluidBusMenu.class, FluidBusMenu::createForServer);
    }
}
