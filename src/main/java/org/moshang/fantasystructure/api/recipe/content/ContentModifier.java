/*
 * Copyright (C) 2026 moshang
 *
 * This file is part of FantasyStructure.
 * Contains code adapted from Multiblocked2 (LGPL-3.0).
 *
 * FantasyStructure is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */

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
