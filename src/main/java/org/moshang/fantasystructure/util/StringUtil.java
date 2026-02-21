package org.moshang.fantasystructure.util;

import com.google.common.base.CaseFormat;

public class StringUtil {
    private StringUtil() {}

    public static String parseStringByChar(String string, char left, char right) {
        return string.substring(string.indexOf(left) + 1, string.indexOf(right));
    }

    public static String formatToLowerCaseUnder(String string) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, string);
    }
}
