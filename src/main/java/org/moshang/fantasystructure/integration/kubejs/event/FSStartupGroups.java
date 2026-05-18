package org.moshang.fantasystructure.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface FSStartupGroups {
    EventGroup REGISTRY = EventGroup.of("FSRegistryEvents");
    EventHandler RECIPE_TYPE = REGISTRY.startup("recipeType", () -> FSRecipeTypeRegistryEventJS.class);
    EventHandler STRUCTURE = REGISTRY.startup("structure", () -> FSStructureDefinitionRegistryEventJS.class);
}