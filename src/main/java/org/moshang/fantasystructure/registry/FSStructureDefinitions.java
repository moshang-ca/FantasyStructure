package org.moshang.fantasystructure.registry;

import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;
import lombok.Getter;
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

    public static void init() {
        DEFINITIONS.unfreeze();
        register("test_controller", "test_structure", "stellar_simulacrum");
        register("ae_storage_controller", "ae_storage_structure", null);
        DEFINITIONS.freeze();
    }

    private static void register(String controllerId, String patternId, String recipeTypeId) {
        DEFINITIONS.register(FantasyStructure.id(controllerId), new StructureDefinition(FantasyStructure.id(patternId), FSRecipes.RECIPE_TYPES.get(recipeTypeId)));
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

        public StructureDefinition(ResourceLocation patternId, ResourceLocation recipeTypeId) {
            this.patternId = patternId;
            if (recipeTypeId == null) {
                this.recipeType = null;
            } else {
                this.recipeType = FSRecipes.RECIPE_TYPES.get(recipeTypeId);

            }
            this.hasRecipeType = this.recipeType != null;
        }

        public List<ItemStack> getMaterials() {
            return SlotUtil.getItemByDefinition(this);
        }

        public static StructureDefinition fromNetwork(FriendlyBufPayload buffer) {
            FriendlyByteBuf buf = buffer.getPayload();
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
