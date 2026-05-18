package org.moshang.fantasystructure.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class FSStructureDefinitionRegistryEventJS extends StartupEventJS {
    private final Map<ResourceLocation, FSStructureDefinitions.DefinitionBuilder> builders = new HashMap<>();

    public FSStructureDefinitions.DefinitionBuilder create(ResourceLocation controllerId, ResourceLocation patternId, ResourceLocation recipeTypeId) {
        FSStructureDefinitions.DefinitionBuilder builder = new FSStructureDefinitions.DefinitionBuilder(controllerId, patternId, recipeTypeId);
        builders.put(controllerId, builder);
        return builder;
    }

    public void removeStructure(ResourceLocation controllerId) {
        builders.remove(controllerId);
        FSStructureDefinitions.DEFINITIONS.remove(controllerId);
    }

    @Nullable
    public FSStructureDefinitions.StructureDefinition getStructure(ResourceLocation controllerId) {
        return FSStructureDefinitions.DEFINITIONS.get(controllerId);
    }

    @Override
    protected void afterPosted(EventResult result) {
        builders.forEach((s, builder) -> builder.build());
    }
}
