package org.moshang.fantasystructure.registry;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.registry.recipe.FSRecipeRegistry;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;
import org.moshang.fantasystructure.util.SlotUtil;

import java.util.List;
import java.util.Map;

public class FSStructureDefinitions {
    public static final FSRecipeRegistry.RL<StructureDefinition> DEFINITIONS = new FSRecipeRegistry.RL<>(FantasyStructure.id("structure_definitions"));
    // Another implementation: use pattern id as key and other as value (as definition)

    public static void init() {
        DEFINITIONS.unfreeze();
        register("test_controller", "test_structure", "stellar_simulacrum");
        DEFINITIONS.freeze();
    }

    private static void register(String controllerId, String patternId, String recipeTypeId) {
        DEFINITIONS.register(FantasyStructure.id(controllerId), new StructureDefinition(FantasyStructure.id(patternId), FSRecipes.RECIPE_TYPES.get(recipeTypeId)));
    }

    @Getter
    @Accessors(fluent = true)
    public static class StructureDefinition {
        private final FSRecipeType recipeType;
        private final ResourceLocation patternId;

        public StructureDefinition(ResourceLocation patternId, FSRecipeType recipeType) {
            this.patternId = patternId;
            this.recipeType = recipeType;
        }

        public List<ItemStack> getMaterials() {
            return SlotUtil.getItemByDefinition(this);
        }
    }
}
