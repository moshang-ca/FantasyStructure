package org.moshang.fantasystructure.api.recipe.content;

import com.google.gson.JsonElement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.moshang.fantasystructure.api.recipe.ingredient.SizedIngredient;

public class SerializerIngredient implements IContentSerializer<Ingredient> {
    public static final SerializerIngredient INSTANCE = new SerializerIngredient();

    private SerializerIngredient() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, Ingredient content) {
        content.toNetwork(buf);
    }

    @Override
    public Ingredient fromNetwork(FriendlyByteBuf buf) {
        return Ingredient.fromNetwork(buf);
    }

    @Override
    public Ingredient fromJson(JsonElement json) {
        return Ingredient.fromJson(json);
    }

    @Override
    public JsonElement toJson(Ingredient content) {
        return content.toJson();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Ingredient of(Object o) {
        if(o instanceof Ingredient ingredient) {
            return ingredient;
        } else if(o instanceof ItemStack itemStack) {
            return SizedIngredient.create(itemStack);
        } else if(o instanceof ItemLike itemLike) {
            return Ingredient.of(itemLike);
        } else if(o instanceof TagKey tagKey) {
            return Ingredient.of(tagKey);
        }
        return Ingredient.EMPTY;
    }

    @Override
    public Ingredient copyWithModifier(Ingredient content, ContentModifier modifier) {
        return content instanceof SizedIngredient sizedIngredient ?
                SizedIngredient.create(sizedIngredient.getInner(), modifier.apply(sizedIngredient.getAmount()).intValue()) :
                SizedIngredient.create(content, modifier.apply(1).intValue());
    }

    @Override
    public Ingredient copyInner(Ingredient content) {
        return SizedIngredient.copy(content);
    }
}
