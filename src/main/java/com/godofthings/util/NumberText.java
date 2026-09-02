package com.godofthings.util;

/**
 * 大数字显示格式化：超过 1000 用 K、M、G、T、P、E 等单位后缀，且去掉小数点、四舍五入到整数，
 * 让文本尽量短（不超过单个物品槽位宽）。例：999 → "999"、1500 → "2K"、999999 → "1M"、1234567890 → "1G"。
 */
public final class NumberText
{
    private static final String[] UNITS = { "K", "M", "G", "T", "P", "E" };

    private NumberText()
    {
    }

    /** 把非负整数格式化为带后缀的紧凑整数文本（1000 以下原样返回）。 */
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
        long n = Math.round(v);
        // 四舍五入到 1000（如 999.6K）时进位到下一个单位，避免出现 "1000K" 这种超长文本
        if (n >= 1000 && unit < UNITS.length - 1)
        {
            v /= 1000.0;
            unit++;
            n = Math.round(v);
        }
        return n + UNITS[unit];
    }
}
