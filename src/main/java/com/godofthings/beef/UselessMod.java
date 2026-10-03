package com.godofthings.beef;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 照抄自 useless_mod 的兼容垫片（shim）。
 *
 * <p>「太初洞见之杖 / 造化垂青之杖」（原 {@code com.godofthings.beef.content.items.EndlessBeafItem}，
 * 该牛排工具框架已于本次死代码清理中整套删除）
 * 这一整套代码是<b>逐字照抄</b>上游 useless_mod 的，唯一改动的只有包名前缀
 * {@code com.sorrowmist.useless} → {@code com.godofthings.beef}。
 * 上游代码里大量出现 {@code UselessMod.MODID} / {@code UselessMod.id(...)} / {@code UselessMod.LOGGER}，
 * 为了不改动任何一行照抄代码，这里保留同名类型，只把 modid 指向本模组。</p>
 *
 * <p>本类<b>不</b>负责注册任何东西——物品、数据组件、网络包、菜单、实体等一律由
 * {@link com.godofthings.Godofthings} 与 {@link com.godofthings.beef.init.BeefRegistration} 接线。</p>
 */
public final class UselessMod
{
    /** 上游为 "useless_mod"；照抄进本模组后统一走本模组的命名空间。 */
    public static final String MODID = "godofthings";

    public static final Logger LOGGER = LoggerFactory.getLogger("Godofthings/BeefTool");

    private UselessMod() {}

    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
