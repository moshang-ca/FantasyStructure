package org.moshang.fantasystructure.util;

public class StringParser {
    public static String parseStringByChar(String blockStateString, char left, char right) {
        return blockStateString.substring(
                blockStateString.indexOf(left) + 1,
                blockStateString.indexOf(right)
        );
    }
}
