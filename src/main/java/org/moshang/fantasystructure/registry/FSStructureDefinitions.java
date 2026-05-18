package org.moshang.fantasystructure.registry;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.recipe.FSRecipeType;
import org.moshang.fantasystructure.registry.recipe.FSRecipeRegistry;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;
import org.moshang.fantasystructure.util.SlotUtil;

import java.util.List;

public class FSStructureDefinitions {
    public static final FSRecipeRegistry.RL<StructureDefinition> DEFINITIONS = new FSRecipeRegistry.RL<>(FantasyStructure.id("structure_definitions"));

    static {
        DEFINITIONS.unfreeze();
        register("test_controller", "test_structure", "stellar_simulacrum");
    }

    public static void register(String controllerId, String patternId, String recipeTypeId) {
        DEFINITIONS.register(FantasyStructure.id(controllerId), new StructureDefinition(FantasyStructure.id(patternId), FSRecipes.RECIPE_TYPES.get(recipeTypeId)));
    }

    public static void register(ResourceLocation controllerId, StructureDefinition definition) {
        DEFINITIONS.register(controllerId, definition);
    }

    @Getter @Setter
    @Accessors(chain = true)
    public static class DefinitionBuilder {
        @Nullable private ResourceLocation recipeTypeId;
        private final ResourceLocation controllerId;
        private ResourceLocation patternId;

        public DefinitionBuilder(ResourceLocation controllerId, ResourceLocation patternId, @Nullable ResourceLocation recipeTypeId) {
            this.controllerId = controllerId;
            this.patternId = patternId;
            this.recipeTypeId = recipeTypeId;
        }

        public void build() {
            if(patternId == null) {
                throw new IllegalArgumentException("You are trying to register a structure without any pattern");
            }
            register(controllerId, new StructureDefinition(patternId, FSRecipes.RECIPE_TYPES.get(recipeTypeId)));
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class StructureDefinition {
        @Nullable private final FSRecipeType recipeType;
        private final ResourceLocation patternId;
        private final boolean hasRecipeType;

        public StructureDefinition(ResourceLocation patternId, @Nullable FSRecipeType recipeType) {
            this.patternId = patternId;
            this.recipeType = recipeType;
            this.hasRecipeType = recipeType != null;
        }

        public List<ItemStack> getMaterials() {
            return SlotUtil.getItems(this);
        }

        public static StructureDefinition fromNetwork(FriendlyByteBuf buf) {
            ResourceLocation patternId = buf.readResourceLocation();
            boolean hasRecipeType = buf.readBoolean();
            FSRecipeType recipeType = null;
            if (hasRecipeType) {
                ResourceLocation recipeTypeId = buf.readResourceLocation();
                recipeType = FSRecipes.RECIPE_TYPES.get(recipeTypeId);
            }
            return new StructureDefinition(patternId, recipeType);
        }

        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.patternId);
            buf.writeBoolean(this.hasRecipeType);
            if (this.hasRecipeType && this.recipeType != null) {
                buf.writeResourceLocation(this.recipeType.getRegistryName());
            }
        }
    }
}
