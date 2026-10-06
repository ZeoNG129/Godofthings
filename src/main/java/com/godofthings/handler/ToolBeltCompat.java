package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

/**
 * 工具皮带（ToolBelt，dev.gigaherz.toolbelt）兼容：<b>开箱即用</b>把本模组的工具类物品放进腰带。
 *
 * <p>ToolBelt 的准入闸门是 {@code ConfigData.isItemStackAllowed}（{@code common/BeltSlot.mayPlace} 调用）：
 * 白名单命中→放行、黑名单命中→拒绝、{@code allowAllNonStackableItems=true} 且不可堆叠→放行、
 * 其余一律拒绝——白名单来自它的 TOML 配置（私有静态集 {@code ConfigData.whiteList}），
 * <b>换新包默认为空</b>，本模组五件可堆叠工具物品会被拦下。</p>
 *
 * <p><b>v5.15.8（按用户要求：换新包不改配置也能放进）</b>：在 ToolBelt 配置重建之后的时机
 * （服务器启动 / 玩家登录兜底）反射把五件物品注入 {@code whiteList} 集合——
 * <b>零配置、零硬依赖</b>：未装 ToolBelt 时直接跳过；用户手动改配置的场合同样并存生效
 * （白名单优先于黑名单，注入条目与配置条目互不冲突）。注入幂等（按 {@code ItemStack.isSameItem} 查重），
 * 集合被配置重载重建后由下一次事件再注入。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class ToolBeltCompat
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TOOLBELT_MODID = "toolbelt";
    private static final String CONFIG_CLASS = "dev.gigaherz.toolbelt.ConfigData";
    private static final String WHITELIST_FIELD = "whiteList";

    private ToolBeltCompat() {}

    /** 注入白名单（反射写 ToolBelt 私有静态集；幂等；集合未建 / 模组未装时安全跳过）。 */
    public static void injectWhitelist()
    {
        if (!ModList.get().isLoaded(TOOLBELT_MODID))
        {
            return; // 未装工具皮带：无需任何处理
        }
        try
        {
            Class<?> config = Class.forName(CONFIG_CLASS);
            Field field = config.getDeclaredField(WHITELIST_FIELD);
            field.setAccessible(true);
            Object raw = field.get(null);
            if (!(raw instanceof Collection<?> whitelist))
            {
                return; // 白名单集合尚未创建（配置未加载）：等下一次时机
            }
            @SuppressWarnings("unchecked")
            Collection<ItemStack> set = (Collection<ItemStack>) whitelist;
            int added = 0;
            for (ItemStack stack : beltStacks())
            {
                if (!containsItem(set, stack))
                {
                    set.add(stack);
                    added++;
                }
            }
            if (added > 0)
            {
                LOGGER.info("[ToolBeltCompat] 已把 {} 件神之工具自动注入工具皮带白名单", added);
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            LOGGER.warn("[ToolBeltCompat] 注入工具皮带白名单失败（ToolBelt 版本可能不兼容），物品需手动加入其配置白名单", e);
        }
    }

    /** 自动放进工具皮带的本模组物品：请神 / 神之更改 / 神之测量 / 神之黑盒 / 神之绑定。 */
    private static List<ItemStack> beltStacks()
    {
        return List.of(
                new ItemStack(Godofthings.GOD_INVITE.get()),
                new ItemStack(Godofthings.GOD_CHANGE.get()),
                new ItemStack(Godofthings.GOD_MEASURE.get()),
                new ItemStack(Godofthings.GOD_BLACK_BOX.get()),
                new ItemStack(Godofthings.GOD_BINDER.get()));
    }

    /** 白名单里是否已有同物品条目（ToolBelt 的匹配口径同为 {@code ItemStack.isSameItem}）。 */
    private static boolean containsItem(Collection<?> whitelist, ItemStack stack)
    {
        for (Object entry : whitelist)
        {
            if (entry instanceof ItemStack existing && ItemStack.isSameItem(existing, stack))
            {
                return true;
            }
        }
        return false;
    }

    // ---- 注入时机（FORGE 总线；都晚于 ToolBelt 的服务器配置重建） ----

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event)
    {
        injectWhitelist(); // 服务器配置此时已重建完毕
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        injectWhitelist(); // 兜底：配置在游戏内被改动 / 重载后再注入
    }
}
