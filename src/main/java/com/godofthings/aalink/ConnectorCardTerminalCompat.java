package com.godofthings.aalink;

import java.lang.reflect.Method;

import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 无线连接器卡 → AE2 无线终端（ae2wtlib）的升级注册桥。
 *
 * <p>ae2wtlib 的 {@code UpgradeHelper.addUpgradeToAllTerminals(ItemLike, int)} 会给
 * 它定义的全部无线终端（含通用终端 WUT）注册升级卡，最终落到 AE2 的
 * {@code Upgrades.add(card, item, count, group)}（group 为无线终端组）。</p>
 *
 * <p>[本地改动] 上游 AE2LT 直接 import 该 API（它把 api 模块内嵌源码）；
 * 本模组不引入 ae2wtlib 编译依赖——API 藏在 ae2wtlib 的 jarjar 嵌套 jar 里，
 * 运行时由 NeoForge 合并进 classpath，因此用反射按全限定名调用；
 * 未装 ae2wtlib 或签名变化时静默跳过。</p>
 */
public final class ConnectorCardTerminalCompat
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String AE2WTLIB_MODID = "ae2wtlib";
    private static final String UPGRADE_HELPER = "de.mari_023.ae2wtlib.api.registration.UpgradeHelper";

    private ConnectorCardTerminalCompat()
    {
    }

    /**
     * 给 ae2wtlib 的全部无线终端注册本卡为升级卡（每台最多 1 张）。
     * 必须在 commonSetup enqueueWork 里调用（物品需已入表）。
     */
    public static void registerAe2wtlibUpgrades(ItemLike card)
    {
        if (!ModList.get().isLoaded(AE2WTLIB_MODID))
        {
            return;
        }
        try
        {
            Class<?> helper = Class.forName(UPGRADE_HELPER);
            Method add = helper.getMethod("addUpgradeToAllTerminals", ItemLike.class, int.class);
            add.invoke(null, card, 1);
            LOGGER.info("Wireless connector card registered as ae2wtlib terminal upgrade");
        }
        catch (Throwable t)
        {
            LOGGER.warn("Failed to register connector card as ae2wtlib upgrade: {}", t.toString());
        }
    }
}
