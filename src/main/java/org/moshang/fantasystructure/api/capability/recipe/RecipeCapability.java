package org.moshang.fantasystructure.api.capability.recipe;

import lombok.Getter;
import org.moshang.fantasystructure.api.recipe.content.ContentModifier;
import org.moshang.fantasystructure.api.recipe.content.IContentSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class RecipeCapability<T> {
    @Getter
    private final String name;
    @Getter
    private final IContentSerializer<T> serializer;

    public RecipeCapability(String name, IContentSerializer<T> serializer) {
        this.name = name;
        this.serializer = serializer;
    }

    public T of(Object o) {
        return serializer.of(o);
    }

    public T copyInner(T content) {
        return serializer.copyInner(content);
    }

    public T copyWithModifier(T content, ContentModifier modifier) {
        return serializer.copyWithModifier(content, modifier);
    }

    @SuppressWarnings("unchecked")
    public final T copyContent(Object content) {
        return copyInner((T) content);
    }

    @SuppressWarnings("unchecked")
    public final T copyContent(Object content, ContentModifier modifier) {
        return copyWithModifier((T) content, modifier);
    }
}
