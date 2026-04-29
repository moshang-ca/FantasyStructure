package org.moshang.fantasystructure.api.recipe.content;

public record ContentModifier(double multiplier, double addition) {
    public static final ContentModifier IDENTITY = new ContentModifier(1, 0);

    public static ContentModifier multiplier(double multiplier) {
        return new ContentModifier(multiplier, 0);
    }

    public Number apply(Number number) {
        return number.doubleValue() * multiplier + addition;
    }
}
