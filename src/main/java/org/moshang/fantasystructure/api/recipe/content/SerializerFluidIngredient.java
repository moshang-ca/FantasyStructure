package org.moshang.fantasystructure.api.recipe.content;

import com.google.gson.JsonElement;
import net.minecraft.network.FriendlyByteBuf;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;

public class SerializerFluidIngredient implements IContentSerializer<FluidIngredient> {
    public static final SerializerFluidIngredient INSTANCE = new SerializerFluidIngredient();

    private SerializerFluidIngredient() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, FluidIngredient content) {
        content.toNetwork(buf);
    }

    @Override
    public FluidIngredient fromNetwork(FriendlyByteBuf buf) {
        return FluidIngredient.fromNetwork(buf);
    }

    @Override
    public FluidIngredient fromJson(JsonElement json) {
        return FluidIngredient.fromJson(json);
    }

    @Override
    public JsonElement toJson(FluidIngredient content) {
        return content.toJson();
    }

    @Override
    public FluidIngredient of(Object o) {
        if(o instanceof FluidIngredient fluidIngredient) {
            return fluidIngredient;
        }
        if(o instanceof FluidStack fluidStack) {
            return FluidIngredient.of(fluidStack);
        }
        return FluidIngredient.EMPTY;
    }

    @Override
    public FluidIngredient copyWithModifier(FluidIngredient content, ContentModifier modifier) {
        if(content.isEmpty()) return content.copy();
        FluidIngredient copy = content.copy();
        copy.setAmount(modifier.apply(copy.getAmount()).intValue());
        return copy;
    }

    @Override
    public FluidIngredient copyInner(FluidIngredient content) {
        return content.copy();
    }
}
