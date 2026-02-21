package org.moshang.fantasystructure.api.recipe.content;

public class ContentModifier {
    public static final ContentModifier IDENTITY = new ContentModifier(1, 0);

    private double multiplier;
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
}
