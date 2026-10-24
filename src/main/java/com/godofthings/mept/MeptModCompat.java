package com.godofthings.mept;

import net.neoforged.fml.ModList;

/**
 * 软依赖检测（照抄上游 ModCompat）。
 */
public final class MeptModCompat
{
    private static Boolean mekanismLoaded = null;
    private static Boolean ae2ltLoaded = null;

    private MeptModCompat()
    {
    }

    public static boolean isMekanismLoaded()
    {
        if (mekanismLoaded == null)
        {
            mekanismLoaded = ModList.get().isLoaded("mekanism");
        }
        return mekanismLoaded;
    }

    public static boolean isAe2ltLoaded()
    {
        if (ae2ltLoaded == null)
        {
            ae2ltLoaded = ModList.get().isLoaded("ae2lt");
        }
        return ae2ltLoaded;
    }
}
