package com.godofthings.util;

import java.util.Locale;

/**
 * 大数字显示格式化：超过 1000 用 K、M、G、T、P 等单位后缀。
 * 例：999 → "999"、1500 → "1.5K"、1000000 → "1M"、1234567890 → "1.2G"。
 */
public final class NumberText
{
    private static final String[] UNITS = { "K", "M", "G", "T", "P", "E" };

    private NumberText()
    {
    }

    /** 把非负整数格式化为带后缀的紧凑文本（1000 以下原样返回）。 */
    public static String format(long value)
    {
        if (value < 1000)
        {
            return String.valueOf(value);
        }
        double v = value;
        int unit = -1;
        while (v >= 1000.0 && unit < UNITS.length - 1)
        {
            v /= 1000.0;
            unit++;
        }
        String s = String.format(Locale.ROOT, "%.1f", v);
        if (s.endsWith(".0"))
        {
            s = s.substring(0, s.length() - 2);
        }
        return s + UNITS[unit];
    }
}
