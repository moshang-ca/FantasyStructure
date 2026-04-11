package org.moshang.fantasystructure.api.recipe.content;

import lombok.Setter;

public class ContentModifier {
    public static final ContentModifier IDENTITY = new ContentModifier(1, 0);

    @Setter
    private double multiplier;
    @Setter
    private double addition;

    public ContentModifier(double multiplier, double addition) {
        this.multiplier = multiplier;
        this.addition = addition;
    }

    public static ContentModifier multiplier(double multiplier) {
        return new ContentModifier(multiplier, 0);
    }

    public Number apply(Number number) {
        return number.doubleValue() * multiplier + addition;
    }

    public void addMultiplier(double multiplier) {
        this.multiplier += multiplier;
    }

    public void addAddition(double addition) {
        this.addition += addition;
    }
}
