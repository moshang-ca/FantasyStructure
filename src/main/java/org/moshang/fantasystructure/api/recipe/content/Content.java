package org.moshang.fantasystructure.api.recipe.content;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;

@Getter
public class Content {
    private Object content;
    private boolean perTick;
    private float chance;
    @NotNull
    private String slotName;

    public Content(Object content, boolean perTick, float chance, @Nullable String slotName) {
        this.content = content;
        this.perTick = perTick;
        this.chance = chance;
        this.slotName = slotName == null ? "" : slotName;
    }

    public Content(Object content, boolean perTick, float chance) {
        this(content, perTick, chance, "");
    }

    public Content copy(RecipeCapability<?> cap, @Nullable ContentModifier modifier) {
        if(modifier == null || chance == 0) {
            return new Content(cap.copyContent(content), perTick, chance, slotName);
        } else {
            return new Content(cap.copyContent(content, modifier), perTick, chance, slotName);
        }
    }
}
